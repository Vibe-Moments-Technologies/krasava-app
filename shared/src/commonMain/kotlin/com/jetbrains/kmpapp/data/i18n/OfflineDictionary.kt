package com.jetbrains.kmpapp.data.i18n

import kotlinx.serialization.json.Json
import kmp_app_template.shared.generated.resources.Res

/**
 * Встроенные офлайн-словари «русский → язык» для надписей меню: кнопки интерфейса
 * и информация, которая задана в приложении локально. Файлы словарей лежат в
 * composeResources/files/dictionaries/<code>.json; меню по ним переводится мгновенно
 * и без интернета. Значения пока заполнены как «слово = слово» и заполняются вручную.
 */
object OfflineDictionary {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val cache = mutableMapOf<AppLanguage, Map<String, String>>()

    /** Пока все словари загружены (меню в кэше, доступно синхронно и офлайн). */
    suspend fun preloadAll() {
        AppLanguage.entries.forEach { language -> menuLabels(language) }
    }

    /** Загружает словарь языка в кэш (если ещё не загружен) и возвращает его. */
    suspend fun menuLabels(language: AppLanguage): Map<String, String> {
        if (language == AppLanguage.RUSSIAN) return emptyMap()
        cache[language]?.let { return it }
        val loaded = try {
            val bytes = Res.readBytes("files/dictionaries/${language.code}.json")
            json.decodeFromString<Map<String, String>>(bytes.decodeToString())
        } catch (e: Exception) {
            println("[i18n] offline dict load failed for ${language.code}: ${e.message}")
            emptyMap()
        }
        cache[language] = loaded
        return loaded
    }

    /** Уже загруженный словарь (синхронно), иначе пусто. */
    fun cached(language: AppLanguage): Map<String, String> = cache[language] ?: emptyMap()
}