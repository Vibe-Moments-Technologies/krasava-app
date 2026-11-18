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
        val DEFAULT = ADD

        fun fromId(id: String?): HeaderButton =
            entries.firstOrNull { it.id == id } ?: DEFAULT
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
 */
internal fun resolveHeaderSlot(
    chosen: HeaderButton,
    hasTarget: Boolean,
    userLessonsEnabled: Boolean,
    qrAvailable: Boolean,
    calendarCollapsed: Boolean
): HeaderButton? = chosen.takeIf {
    it in availableHeaderButtons(hasTarget, userLessonsEnabled, qrAvailable, calendarCollapsed)
}