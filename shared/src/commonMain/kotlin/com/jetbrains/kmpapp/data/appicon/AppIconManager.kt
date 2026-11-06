package com.jetbrains.kmpapp.data.appicon

/**
 * Выбор иконки приложения. На Android выбор реализуется четырьмя activity-alias
 * (основная/Грифон × светлая/тёмная): движок включает alias, совпадающий с
 * иконкой и эффективной темой, и отключает остальные. На iOS — setAlternateIconName
 * (asset catalog сам подставляет светлый/тёмный вариант).
 *
 * «Тема иконки» — режим, выбранный пользователем (SYSTEM/LIGHT/DARK), а не
 * системная тема: режим «По умолчанию» + тёмная тема ОС => тёмная иконка.
 */
object AppIconManager {
    /** Базовая иконка (default = nil для iOS asset catalog). */
    const val ICON_DEFAULT = "default"
    /** Альтернативная иконка «Грифон». */
    const val ICON_ALT = "AppIconAlt"

    interface IconEngine {
        fun applyIcon(name: String, dark: Boolean)
    }

    var engine: IconEngine? = null
        private set

    /** Режим темы для иконки: null = SYSTEM (определяется через isDark). */
    var iconDark: Boolean? = null
        private set

    /** true там, где платформа умеет менять иконку (сейчас iOS и Android). */
    val supportsSwitching: Boolean get() = engine != null

    fun setEngine(newEngine: IconEngine) {
        engine = newEngine
    }

    /** Вызывается при смене иконки или темы приложения. */
    fun apply(name: String, dark: Boolean? = iconDark) {
        iconDark = dark
        engine?.applyIcon(name, dark ?: false)
    }
}
