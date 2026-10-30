package com.jetbrains.kmpapp.data.storage

import com.jetbrains.kmpapp.data.model.LessonNote
import com.jetbrains.kmpapp.data.model.NoteScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * Хранилище заметок, привязанных к расписанию (R2).
 *
 * Ключи:
 *  - LESSON:  "{targetId}_{date}_{bellNumber}" — заметка к конкретной паре
 *  - SUBJECT: "{targetId}_{subject.lowercase()}" — заметка к предмету
 *
 * Персистентность: файл lesson_notes.json в documents/files-папке
 * ([NotesFileStorage]) — заметки это пользовательский контент, не кеш:
 * они переживают «очистить кеш», «сбросить данные» и пересоздание
 * расписания. Раньше жили в PlatformStorage и терялись при clearAll().
 *
 * Миграция: при первом чтении заметки из старого ключа prefs
 * (krasava_lesson_notes) переезжают в файл, ключ снимается.
 */
class LessonNotesStorage(
    private val legacyStore: KeyValueStore,
    private val fileStorage: NotesFileStorage
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _notes = MutableStateFlow<List<LessonNote>>(emptyList())
    val notes: StateFlow<List<LessonNote>> = _notes.asStateFlow()

    init {
        load()
    }

    private fun load() {
        try {
            val fromFile = fileStorage.read()
            if (!fromFile.isNullOrBlank()) {
                _notes.value = json.decodeFromString(fromFile)
                return
            }
            // Миграция со старого ключа prefs: заметки, записанные до
            // перевода на файл, не должны потеряться.
            val legacy = legacyStore.getString(KEY_LEGACY)
            if (!legacy.isNullOrBlank()) {
                val parsed = json.decodeFromString<List<LessonNote>>(legacy)
                _notes.value = parsed
                fileStorage.write(json.encodeToString(parsed))
                legacyStore.remove(KEY_LEGACY)
            }
        } catch (t: Throwable) {
            println("LessonNotesStorage: failed to load: ${t.message}")
        }
    }

    private fun persist() {
        scope.launch {
            try {
                fileStorage.write(json.encodeToString(_notes.value))
            } catch (e: Exception) {
                println("LessonNotesStorage: failed to persist: ${e.message}")
            }
        }
    }

    /**
     * Синхронная запись файла — только для тестов: persist() асинхронный,
     * а тесту нужен файл сразу после save-вызова.
     */
    internal fun persistNow() {
        try {
            fileStorage.write(json.encodeToString(_notes.value))
        } catch (e: Exception) {
            println("LessonNotesStorage: failed to persist: ${e.message}")
        }
    }

    // ── Ключи ─────────────────────────────────────────────────

    fun lessonKey(targetId: Int, date: String, bellNumber: Int): String =
        "${targetId}_${date}_$bellNumber"

    fun subjectKey(targetId: Int, subject: String): String =
        "${targetId}_${subject.trim().lowercase()}"

    // ── Чтение ────────────────────────────────────────────────

    fun getLessonNote(targetId: Int, date: String, bellNumber: Int): LessonNote? =
        _notes.value.firstOrNull { it.noteKey == lessonKey(targetId, date, bellNumber) }

    fun getSubjectNote(targetId: Int, subject: String): LessonNote? =
        _notes.value.firstOrNull { it.noteKey == subjectKey(targetId, subject) }

    fun getNotesForTarget(targetId: Int): List<LessonNote> =
        _notes.value.filter { it.targetId == targetId }

    // ── Запись ────────────────────────────────────────────────

    fun saveLessonNote(targetId: Int, date: String, bellNumber: Int, text: String) {
        val key = lessonKey(targetId, date, bellNumber)
        val now = Clock.System.now().toEpochMilliseconds()
        val existing = _notes.value.firstOrNull { it.noteKey == key }
        val note = LessonNote(
            id = existing?.id ?: now.toString(),
            targetId = targetId,
            scope = NoteScope.LESSON,
            noteKey = key,
            text = text,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        _notes.update { list ->
            list.filter { it.noteKey != key } + note
        }
        persist()
    }

    fun saveSubjectNote(targetId: Int, subject: String, text: String) {
        val key = subjectKey(targetId, subject)
        val now = Clock.System.now().toEpochMilliseconds()
        val existing = _notes.value.firstOrNull { it.noteKey == key }
        val note = LessonNote(
            id = existing?.id ?: now.toString(),
            targetId = targetId,
            scope = NoteScope.SUBJECT,
            noteKey = key,
            text = text,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        _notes.update { list ->
            list.filter { it.noteKey != key } + note
        }
        persist()
    }

    fun deleteNote(noteId: String) {
        _notes.update { list -> list.filter { it.id != noteId } }
        persist()
    }

    // ── Каскадное удаление ────────────────────────────────────

    /**
     * Удаляет все заметки расписания — только явное удаление расписания
     * пользователем (removeTarget): заметки переживают обновления,
     * сброс данных и очистку кеша.
     */
    fun removeNotesForTarget(targetId: Int) {
        _notes.update { list -> list.filter { it.targetId != targetId } }
        persist()
    }

    /** Удаляет заметки к прошедшим парам (старше 90 дней). */
    fun cleanOldLessonNotes(beforeEpochMs: Long): Int {
        val old = _notes.value.filter {
            it.scope == NoteScope.LESSON && it.updatedAt < beforeEpochMs
        }
        if (old.isNotEmpty()) {
            _notes.update { list -> list.filter { it !in old } }
            persist()
        }
        return old.size
    }

    /** Размер данных в байтах (для DataAndCacheScreen). */
    fun getStorageSizeBytes(): Long =
        fileStorage.read()?.encodeToByteArray()?.size?.toLong() ?: 0L

    fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes Б"
        bytes < 1024 * 1024 -> "${bytes / 1024} КБ"
        else -> "${(bytes * 10 / (1024 * 1024)) / 10.0} МБ"
    }

    companion object {
        /** Старый ключ PlatformStorage — для одноразовой миграции в файл. */
        internal const val KEY_LEGACY = "krasava_lesson_notes"
    }
}
