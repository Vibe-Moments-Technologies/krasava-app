package com.jetbrains.kmpapp.data.i18n

import kotlinx.serialization.Serializable

/**
 * Поддерживаемые языки приложения. Русский — источник переводов и по умолчанию.
 * Перевод всегда идёт с русского: каскад «прошлый язык → новый» запрещён.
 */
@Serializable
enum class AppLanguage(val code: String, val nativeName: String) {
    RUSSIAN("ru", "Русский"),
    ENGLISH("en", "English"),
    POLISH("pl", "Polski"),
    GERMAN("de", "Deutsch"),
    FRENCH("fr", "Français"),
    SPANISH("es", "Español"),
    ITALIAN("it", "Italiano");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: RUSSIAN
    }
}
