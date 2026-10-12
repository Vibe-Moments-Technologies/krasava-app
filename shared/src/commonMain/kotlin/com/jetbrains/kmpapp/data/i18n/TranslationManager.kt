package com.jetbrains.kmpapp.data.i18n

import com.jetbrains.kmpapp.data.model.Lesson
import com.jetbrains.kmpapp.data.storage.PlatformStorage
import com.jetbrains.kmpapp.data.storage.ScheduleStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.time.TimeSource

/**
 * Язык приложения: выбор языка, перевод надписей интерфейса и названий предметов
 * расписания. Перевод всегда идёт с русского источника — каскад «прошлый язык →
 * новый» запрещён. Русский работает локально и без интернета.
 */
class TranslationManager(
    private val api: TranslationApi,
    private val platformStorage: PlatformStorage,
    private val scheduleStorage: ScheduleStorage
) {
    data class State(
        val language: AppLanguage = AppLanguage.RUSSIAN,
        val labels: Map<String, String> = emptyMap(),
        val isPreparing: Boolean = false,
        /** Текущий этап перевода: label — этап (надписи/расписание), done/total — прогресс. */
        val progress: Progress? = null,
        val lastError: String? = null,
        val lastReport: Report? = null
    )

    data class Progress(val label: String, val done: Int, val total: Int)

    /** Отчёт о последнем переводе надписей: сколько успешно/неудачно и за какое время. */
    data class Report(
        val total: Int,
        val ok: Int,
        val failed: Int,
        val millis: Long
    )

    data class BatchResult(val map: Map<String, String>, val failed: Int, val total: Int, val millis: Long) {
        val ok: Int get() = total - failed
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }
    private val _state = MutableStateFlow(
        State(language = AppLanguage.fromCode(platformStorage.getString(KEY_LANGUAGE)))
    )
    val state: StateFlow<State> = _state.asStateFlow()

    /**
     * Перевод русского источника в текущий язык. Русский и ещё не переведённые
     * строки возвращаются как есть — интерфейс никогда не «ломается».
     */
    fun t(ru: String): String {
        val current = _state.value
        return if (current.language == AppLanguage.RUSSIAN) ru else current.labels[ru] ?: ru
    }

    fun selectLanguage(language: AppLanguage) {
        if (language == _state.value.language) return
        scope.launch { runCatching { platformStorage.saveString(KEY_LANGUAGE, language.code) } }
        // Сразу подставляем сохранённый кэш переводов (если есть), дальше —
        // свежий перевод в фоне. Язык переключаем мгновенно, UI отзывчив.
        val cached = if (language == AppLanguage.RUSSIAN) emptyMap() else loadLabelsCache(language)
        _state.value = State(language = language, labels = cached)
        if (language == AppLanguage.RUSSIAN) return

        scope.launch {
            _state.update { it.copy(isPreparing = true, lastError = null, progress = null) }
            val batch = translateBatch(Labels.UI, language) { done, total ->
                _state.update { it.copy(progress = Progress("Надписи", done, total)) }
            }
            _state.update {
                it.copy(
                    labels = it.labels + batch.map,
                    lastReport = Report(batch.total, batch.ok, batch.failed, batch.millis),
                    lastError = if (batch.failed > 0) "Не удалось перевести часть надписей (${batch.failed})" else null
                )
            }
            persistLabelsCache(language, _state.value.labels)
            println("[i18n] ui-labels lang=${language.code} total=${batch.total} ok=${batch.ok} fail=${batch.failed} t=${batch.millis}ms")
            translateAllSavedSchedulesSync(language)
            _state.update { it.copy(isPreparing = false, progress = null) }
        }
    }

    /**
     * Переводит список строк с русского на [target]. Строки группируются в батчи
     * по несколько штук в одном запросе (MyMemory отдаёт построчно в том же
     * порядке) — вместо десятков одиночных запросов выходит несколько. Группы
     * гонятся волнами параллельно. Неудачные остаются русскими.
     */
    suspend fun translateBatch(
        texts: List<String>,
        target: AppLanguage,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): BatchResult {
        val distinct = texts.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (target == AppLanguage.RUSSIAN) {
            onProgress(distinct.size, distinct.size)
            return BatchResult(distinct.associateWith { it }, failed = 0, total = distinct.size, millis = 0)
        }
        val started = TimeSource.Monotonic.markNow()
        val map = linkedMapOf<String, String>()
        var failed = 0
        var done = 0
        val groups = buildGroups(distinct)

        onProgress(0, distinct.size)
        coroutineScope {
            groups.chunked(MAX_GROUPS_PER_WAVE).forEach { wave ->
                val results = wave.map { group -> async { translateGroup(group, target) } }.awaitAll()
                results.forEach { (groupMap, groupFailed, groupSize) ->
                    map.putAll(groupMap)
                    failed += groupFailed
                    done += groupSize
                }
                onProgress(done, distinct.size)
                // Пауза между волнами запросов: щадим бесплатный лимит API.
                delay(BATCH_DELAY_MILLIS)
            }
        }
        return BatchResult(map, failed, distinct.size, started.elapsedNow().inWholeMilliseconds)
    }

    /** Переводит группу строк одним запросом; при рассинхроне строк — по одной. */
    private suspend fun translateGroup(
        group: List<String>,
        target: AppLanguage
    ): Triple<Map<String, String>, Int, Int> {
        val joined = group.joinToString("\n")
        val raw = try { api.translateRaw(joined, target) } catch (_: Exception) { null }
        val lines = raw?.split("\n")
        if (raw != null && lines != null && lines.size == group.size) {
            val out = linkedMapOf<String, String>()
            var failed = 0
            group.forEachIndexed { index, text ->
                val translated = lines[index].trim()
                if (translated.isEmpty()) failed++ else out[text] = translated
            }
            return Triple(out, failed, group.size)
        }
        val out = linkedMapOf<String, String>()
        var failed = 0
        group.forEach { text ->
            try {
                val translated = api.translate(text, target).trim()
                if (translated.isEmpty()) failed++ else out[text] = translated
            } catch (_: Exception) {
                failed++
            }
        }
        return Triple(out, failed, group.size)
    }

    /** Собирает строки в группы, не превышающие лимит символов/строк одного запроса. */
    private fun buildGroups(items: List<String>): List<List<String>> {
        val groups = mutableListOf(mutableListOf<String>())
        var length = 0
        for (item in items) {
            val size = item.length + 1
            val last = groups.last()
            if (last.isNotEmpty() && (length + size > MAX_BATCH_CHARS || last.size >= MAX_BATCH_LINES)) {
                groups.add(mutableListOf())
                length = 0
            }
            groups.last().add(item)
            length += size
        }
        return groups
    }

    /**
     * Создаёт/обновляет JSON-копию расписания на текущем языке. Возвращает
     * переведённый список либо null (русский язык / сбой перевода — ошибка в стейт).
     */
    suspend fun translateScheduleCopy(
        targetId: Int,
        russianLessons: List<Lesson>,
        label: String = "Расписание",
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): List<Lesson>? {
        val language = _state.value.language
        if (language == AppLanguage.RUSSIAN) return null
        return try {
            val batch = translateBatch(russianLessons.map { it.subject }, language, onProgress)
            val translated = russianLessons.map { lesson ->
                batch.map[lesson.subject]?.let { lesson.copy(subject = it) } ?: lesson
            }
            scheduleStorage.saveTranslatedLessons(targetId, language, translated)
            // Регистрируем переводы предметов: карточки расписания читают их через t().
            _state.update { it.copy(labels = it.labels + batch.map) }
            persistLabelsCache(language, _state.value.labels)
            println("[i18n] schedule targetId=$targetId label=\"$label\" lang=${language.code} subjects=${batch.total} ok=${batch.ok} fail=${batch.failed} t=${batch.millis}ms")
            if (batch.failed > 0) {
                _state.update { it.copy(lastError = "Не удалось перевести названия предметов (${batch.failed})") }
            }
            translated
        } catch (e: Exception) {
            _state.update { it.copy(lastError = "Не удалось перевести расписание: ${e.message}") }
            null
        }
    }

    fun translateAllSavedSchedules(language: AppLanguage) {
        scope.launch { translateAllSavedSchedulesSync(language) }
    }

    private suspend fun translateAllSavedSchedulesSync(language: AppLanguage) {
        if (language == AppLanguage.RUSSIAN) return
        for (target in scheduleStorage.savedTargets.value) {
            val lessons = scheduleStorage.getLessons(target.id)
            if (lessons.isNullOrEmpty()) continue
            val label = "Расписание: ${target.targetTitle}"
            translateScheduleCopy(target.id, lessons, label) { done, total ->
                _state.update { it.copy(progress = Progress(label, done, total)) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(lastError = null) }
    }

    private fun loadLabelsCache(language: AppLanguage): Map<String, String> = try {
        val s = platformStorage.getString(KEY_LABELS_PREFIX + language.code)
        if (s.isNullOrBlank()) emptyMap()
        else try { json.decodeFromString<Map<String, String>>(s) } catch (_: Throwable) { emptyMap() }
    } catch (_: Throwable) {
        emptyMap()
    }

    private fun persistLabelsCache(language: AppLanguage, labels: Map<String, String>) {
        if (labels.isEmpty()) return
        scope.launch {
            try {
                platformStorage.saveString(KEY_LABELS_PREFIX + language.code, json.encodeToString(labels))
            } catch (e: Exception) {
                println("[i18n] labels cache persist failed: ${e.message}")
            }
        }
    }

    private companion object {
        const val KEY_LANGUAGE = "krasava_app_language"
        const val KEY_LABELS_PREFIX = "krasava_labels_"
        // Одна «волна» батч-запросов к MyMemory; в каждом — несколько строк.
        const val MAX_BATCH_LINES = 10
        const val MAX_BATCH_CHARS = 350
        const val MAX_GROUPS_PER_WAVE = 4
        const val BATCH_DELAY_MILLIS = 100L
    }
}