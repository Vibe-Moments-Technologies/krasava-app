package com.jetbrains.kmpapp.data.model

/**
 * Кнопка, которую можно поставить в шапку расписания.
 *
 * Владелец: можно выбрать до ТРЁХ любых кнопок, включая кнопку меню.
 * В меню (пузырьке) показываются все НЕ выбранные доступные кнопки.
 */
enum class HeaderButton(val id: String) {
    /** «Добавить расписание» — плюс. */
    ADD("add"),

    /** «Своя пара» — календарик с карандашом. */
    USER_LESSON("user_lesson"),

    /** Сканер QR — всегда доступен, отдельного тумблера больше нет. */
    QR("qr"),

    /** Кружок с числом дня: применим, когда лента календаря свёрнута. */
    DAY_BADGE("day_badge"),

    /** Кнопка меню: раскрывает пузырёк с невыбранными кнопками. */
    MENU("menu");

    companion object {
        /** Старый одиночный слот: дефолт «плюс + меню» сохраняем. */
        val LEGACY_DEFAULT: HeaderButton = ADD

        /** Дефолт нового списка: плюс и меню. */
        val DEFAULT_SELECTION: List<HeaderButton> = listOf(ADD, MENU)

        /** Максимум кнопок в шапке (решение владельца). */
        const val MAX_VISIBLE = 3

        const val EMPTY_ID = "none"

        /** Разбор старого одиночного значения (для миграции). */
        fun fromStoredId(id: String?): HeaderButton? = when {
            id.isNullOrBlank() -> LEGACY_DEFAULT
            id == EMPTY_ID -> null
            else -> entries.firstOrNull { it.id == id }
        }

        fun fromId(id: String): HeaderButton? = entries.firstOrNull { it.id == id }

        /** Сериализация списка в хранилище: `add,qr,menu`. */
        fun encodeSelection(buttons: List<HeaderButton>): String =
            buttons.distinct().take(MAX_VISIBLE).joinToString(",") { it.id }

        fun decodeSelection(raw: String?): List<HeaderButton>? {
            if (raw.isNullOrBlank()) return null
            return raw.split(',')
                .mapNotNull { fromId(it.trim()) }
                .distinct()
                .take(MAX_VISIBLE)
        }
    }
}

/**
 * Все кнопки, применимые сейчас.
 *
 * QR всегда доступен: владелец попросил убрать отдельный внешний тумблер и
 * сделать кнопку обычным пунктом в настройках шапки.
 */
internal fun availableHeaderButtons(
    hasTarget: Boolean,
    userLessonsEnabled: Boolean,
    calendarCollapsed: Boolean
): List<HeaderButton> = buildList {
    add(HeaderButton.ADD)
    if (hasTarget && userLessonsEnabled) add(HeaderButton.USER_LESSON)
    add(HeaderButton.QR)
    if (hasTarget && calendarCollapsed) add(HeaderButton.DAY_BADGE)
    add(HeaderButton.MENU)
}

/**
 * Выбранные кнопки, очищенные от неприменимых и лишних.
 * Порядок сохраняем заданный пользователем, дубли убираем.
 */
internal fun normalizeHeaderSelection(
    selected: List<HeaderButton>,
    available: List<HeaderButton>
): List<HeaderButton> = selected.distinct().filter { it in available }.take(HeaderButton.MAX_VISIBLE)

/** Кнопки для пузырька: ВСЕ доступные, кроме выбранных. МЕНЮ внутри не показываем. */
internal fun bubbleHeaderButtons(
    selected: List<HeaderButton>,
    available: List<HeaderButton>
): List<HeaderButton> = available.filterNot { it in selected || it == HeaderButton.MENU }
