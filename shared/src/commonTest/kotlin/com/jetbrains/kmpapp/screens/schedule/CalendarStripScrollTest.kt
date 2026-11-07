package com.jetbrains.kmpapp.screens.schedule

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Окно видимости ленты календаря ([calendarStripNeedsScroll]).
 *
 * Регресс с устройства владельца: в режиме «две недели» тап по дню второй
 * строки перелистывал ленту, делая эту неделю первой, — хотя она уже была на
 * экране. Старая проверка сравнивала только с первой строкой, поэтому вторая
 * строка считалась «невидимой». Тесты фиксируют, что окно = фактическая
 * вёрстка: 1 неделя в WEEK, 2 в TWO_WEEKS.
 */
class CalendarStripScrollTest {

    // ── Режим одной недели: видно ровно текущую строку ──────────────────

    @Test
    fun oneWeek_selectedWeekIsCurrent_noScroll() {
        assertFalse(calendarStripNeedsScroll(weeksAhead = 0, twoWeeksMode = false))
    }

    @Test
    fun oneWeek_selectedWeekIsNext_scroll() {
        assertTrue(calendarStripNeedsScroll(weeksAhead = 1, twoWeeksMode = false))
    }

    @Test
    fun oneWeek_selectedWeekIsPrevious_scroll() {
        assertTrue(calendarStripNeedsScroll(weeksAhead = -1, twoWeeksMode = false))
    }

    // ── Режим двух недель: вторая строка тоже видна ─────────────────────

    /** Тот самый баг: выбрана неделя второй строки — листать не надо. */
    @Test
    fun twoWeeks_selectedWeekIsSecondRow_noScroll() {
        assertFalse(calendarStripNeedsScroll(weeksAhead = 1, twoWeeksMode = true))
    }

    @Test
    fun twoWeeks_selectedWeekIsFirstRow_noScroll() {
        assertFalse(calendarStripNeedsScroll(weeksAhead = 0, twoWeeksMode = true))
    }

    @Test
    fun twoWeeks_selectedWeekIsBeyondSecondRow_scroll() {
        assertTrue(calendarStripNeedsScroll(weeksAhead = 2, twoWeeksMode = true))
    }

    @Test
    fun twoWeeks_selectedWeekIsBeforeFirstRow_scroll() {
        assertTrue(calendarStripNeedsScroll(weeksAhead = -1, twoWeeksMode = true))
    }

    // ── Дальние переходы (пикер месяца, свайп списка пар) ───────────────

    @Test
    fun farJump_scrollInBothModes() {
        listOf(4, 12, -8, -40).forEach { weeks ->
            assertTrue(
                calendarStripNeedsScroll(weeks, twoWeeksMode = false),
                "WEEK: $weeks недель в стороне должен требовать прокрутки"
            )
            assertTrue(
                calendarStripNeedsScroll(weeks, twoWeeksMode = true),
                "TWO_WEEKS: $weeks недель в стороне должен требовать прокрутки"
            )
        }
    }

    // ── Расхождение режимов: одна и та же неделя видна по-разному ───────

    /**
     * Ключевое свойство фикса: неделя +1 в TWO_WEEKS видна, а в WEEK — нет.
     * Если вёрстку когда-нибудь расширят до трёх строк, этот тест напомнит,
     * что окно видимости надо править вместе с ней.
     */
    @Test
    fun secondRowVisibilityDiffersBetweenModes() {
        val weekAhead = 1
        assertFalse(calendarStripNeedsScroll(weekAhead, twoWeeksMode = true))
        assertTrue(calendarStripNeedsScroll(weekAhead, twoWeeksMode = false))
    }

    @Test
    fun visibleWindowSizeMatchesExpandState() {
        // Границы окна: последний видимый сдвиг и первый невидимый.
        assertEquals(false, calendarStripNeedsScroll(0, twoWeeksMode = false))
        assertEquals(true, calendarStripNeedsScroll(1, twoWeeksMode = false))
        assertEquals(false, calendarStripNeedsScroll(1, twoWeeksMode = true))
        assertEquals(true, calendarStripNeedsScroll(2, twoWeeksMode = true))
    }
}
