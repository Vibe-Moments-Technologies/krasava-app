package com.jetbrains.kmpapp.data.model

/**
 * Кнопка, которую можно поставить в шапку расписания рядом с меню.
 *
 * Репорт владельца: в шапке стабильно помещаются только две кнопки справа
 * (учитывая выборщик расписания и бейдж изменений) — при большем числе
 * шапка переполняется. Решение: одна кнопка + кнопка меню (меню всегда
 * крайнее правое), остальные живут в пузырьковом меню.
 *
 * Слот один: включение другой кнопки автоматически выключает предыдущую.
 */
enum class HeaderButton(val id: String) {
    /** «Добавить расписание» — плюс. Дефолт. */
    ADD("add"),

    /** «Своя пара» — календарик с карандашом. */
    USER_LESSON("user_lesson"),

    /** Сканер QR. */
    QR("qr"),

    /** Кружок с числом дня: виден только при свёрнутой ленте календаря. */
    DAY_BADGE("day_badge");

    companion object {
        /** Дефолт владельца: в слоте стоит «Добавить расписание». */
        val DEFAULT: HeaderButton = ADD

        /** Значение хранилища для пустого слота: все допы выключены, в шапке только меню. */
        const val EMPTY_ID = "none"

        /**
         * Разбор сохранённого значения слота.
         *
         * null — слот пуст (владелец выключил все допы) либо значение битое:
         * неизвестный id не должен молча превращаться в «плюс». Отсутствие
         * ключа — свежая установка, там дефолт.
         */
        fun fromStoredId(id: String?): HeaderButton? = when {
            id.isNullOrBlank() -> DEFAULT
            id == EMPTY_ID -> null
            else -> entries.firstOrNull { it.id == id }
        }

        /** Что писать в хранилище: id кнопки или [EMPTY_ID] для пустого слота. */
        fun toStoredId(button: HeaderButton?): String = button?.id ?: EMPTY_ID
    }
}

/**
 * Кнопки, которые есть смысл показывать в меню пузырька прямо сейчас.
 *
 * Контекстные кнопки не появляются, когда действие недоступно: «Своя пара»
 * требует выбранного расписания и включённой настройки, QR — доступного
 * сканера. Кружок дня показывается только при свёрнутой ленте календаря:
 * в развёрнутом состоянии число дня и так видно в ленте.
 */
internal fun availableHeaderButtons(
    hasTarget: Boolean,
    userLessonsEnabled: Boolean,
    qrAvailable: Boolean,
    calendarCollapsed: Boolean
): List<HeaderButton> = buildList {
    add(HeaderButton.ADD)
    if (hasTarget && userLessonsEnabled) add(HeaderButton.USER_LESSON)
    if (qrAvailable) add(HeaderButton.QR)
    if (hasTarget && calendarCollapsed) add(HeaderButton.DAY_BADGE)
}

/**
 * Что реально рисуется в слоте шапки: выбранная кнопка, если она сейчас
 * применима, иначе пусто (в шапке остаётся только кнопка меню).
 *
 * [chosen] == null — владелец выключил ВСЕ допы: в шапке только меню, а все
 * кнопки живут в пузырьке. Это отдельное осознанное состояние, а не «дефолт»:
 * раньше выключение последней кнопки молча возвращало «плюс».
 */
internal fun resolveHeaderSlot(
    chosen: HeaderButton?,
    hasTarget: Boolean,
    userLessonsEnabled: Boolean,
    qrAvailable: Boolean,
    calendarCollapsed: Boolean
): HeaderButton? = chosen?.takeIf {
    it in availableHeaderButtons(hasTarget, userLessonsEnabled, qrAvailable, calendarCollapsed)
}