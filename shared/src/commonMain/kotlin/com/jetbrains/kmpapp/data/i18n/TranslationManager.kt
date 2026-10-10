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
        val lastError: String? = null,
        val lastReport: Report? = null
    )

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
            _state.update { it.copy(isPreparing = true, lastError = null) }
            val batch = translateBatch(Labels.UI, language)
            _state.update { current ->
                current.copy(
                    labels = current.labels + batch.map,
                    isPreparing = false,
                    lastReport = Report(batch.total, batch.ok, batch.failed, batch.millis),
                    lastError = if (batch.failed > 0) "Не удалось перевести часть надписей (${batch.failed})" else null
                )
            }
            persistLabelsCache(language, _state.value.labels)
            println("[i18n] ui-labels lang=${language.code} total=${batch.total} ok=${batch.ok} fail=${batch.failed} t=${batch.millis}ms")
            translateAllSavedSchedules(language)
        }
    }

    /**
     * Переводит список строк с русского на [target] параллельно (небольшими
     * волнами, чтобы не упереться в лимит MyMemory). Неудачные остаются русскими.
     */
    suspend fun translateBatch(texts: List<String>, target: AppLanguage): BatchResult {
        val distinct = texts.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (target == AppLanguage.RUSSIAN) {
            return BatchResult(distinct.associateWith { it }, failed = 0, total = distinct.size, millis = 0)
        }
        val started = TimeSource.Monotonic.markNow()
        val map = linkedMapOf<String, String>()
        var failed = 0

        coroutineScope {
            distinct.chunked(CONCURRENCY).forEach { chunk ->
                val results = chunk.map { text ->
                    async {
                        text to runCatching { api.translate(text, target).trim() }
                    }
                }.awaitAll()
                results.forEach { (text, result) ->
                    result.onSuccess { map[text] = it }.onFailure {
                        failed++
                        map[text] = text
                    }
                }
                // Пауза между волнами запросов: защита лимита бесплатного API.
                delay(BATCH_DELAY_MILLIS)
            }
        }
        return BatchResult(map, failed, distinct.size, started.elapsedNow().inWholeMilliseconds)
    }

    /**
     * Создаёт/обновляет JSON-копию расписания на текущем языке. Возвращает
     * переведённый список либо null (русский язык / сбой перевода — ошибка в стейт).
     */
    suspend fun translateScheduleCopy(targetId: Int, russianLessons: List<Lesson>): List<Lesson>? {
        val language = _state.value.language
        if (language == AppLanguage.RUSSIAN) return null
        return try {
            val batch = translateBatch(russianLessons.map { it.subject }, language)
            val translated = russianLessons.map { lesson ->
                batch.map[lesson.subject]?.let { lesson.copy(subject = it) } ?: lesson
            }
            scheduleStorage.saveTranslatedLessons(targetId, language, translated)
            // Регистрируем переводы предметов: карточки расписания читают их через t().
            _state.update { it.copy(labels = it.labels + batch.map) }
            persistLabelsCache(language, _state.value.labels)
            println("[i18n] schedule targetId=$targetId lang=${language.code} subjects=${batch.total} ok=${batch.ok} fail=${batch.failed} t=${batch.millis}ms")
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
        if (language == AppLanguage.RUSSIAN) return
        scope.launch {
            for (target in scheduleStorage.savedTargets.value) {
                val lessons = scheduleStorage.getLessons(target.id)
                if (!lessons.isNullOrEmpty()) {
                    translateScheduleCopy(target.id, lessons)
                }
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
        // Одна «волна» параллельных запросов к MyMemory.
        const val CONCURRENCY = 4
        const val BATCH_DELAY_MILLIS = 250L
    }
}