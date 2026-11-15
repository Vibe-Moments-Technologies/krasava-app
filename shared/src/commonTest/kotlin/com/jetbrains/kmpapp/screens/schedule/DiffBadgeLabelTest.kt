package com.jetbrains.kmpapp.screens.schedule

import com.jetbrains.kmpapp.data.model.LessonDiffItem
import com.jetbrains.kmpapp.data.model.LessonDiffType
import com.jetbrains.kmpapp.data.model.ScheduleDiff
import com.jetbrains.kmpapp.data.model.badgeLabel
import com.jetbrains.kmpapp.data.model.changesCountLabel
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * «±N» в шапке и «N изменений» в шторке.
 *
 * Репорт владельца: бейдж «Diff (147)» при нескольких кнопках в шапке
 * «корёжится» — заменён на знак плюс-минус и ограниченное число.
 */
class DiffBadgeLabelTest {

    private fun diff(itemCount: Int): ScheduleDiff {
        val item = LessonDiffItem(
            type = LessonDiffType.ADDED,
            date = LocalDate(2026, 11, 16),
            bellNumber = 1,
            subject = "Тест",
            description = "Новая пара"
        )
        return ScheduleDiff(
            targetId = 1,
            targetTitle = "ИКБО-01-20",
            items = List(itemCount) { item }
        )
    }

    @Test
    fun badgeShowsCountWithPlusMinusSign() {
        assertEquals("±0", diff(0).badgeLabel())
        assertEquals("±1", diff(1).badgeLabel())
        assertEquals("±3", diff(3).badgeLabel())
        assertEquals("±42", diff(42).badgeLabel())
        assertEquals("±99", diff(99).badgeLabel())
    }

    @Test
    fun badgeCapsThreeDigitCounts() {
        assertEquals("±99+", diff(100).badgeLabel())
        assertEquals("±99+", diff(147).badgeLabel())
        assertEquals("±99+", diff(1000).badgeLabel())
    }

    @Test
    fun sheetCountsUseRussianPlurals() {
        assertEquals("1 изменение", changesCountLabel(1))
        assertEquals("2 изменения", changesCountLabel(2))
        assertEquals("3 изменения", changesCountLabel(3))
        assertEquals("4 изменения", changesCountLabel(4))
        assertEquals("5 изменений", changesCountLabel(5))
        assertEquals("11 изменений", changesCountLabel(11))
        assertEquals("21 изменение", changesCountLabel(21))
        assertEquals("101 изменение", changesCountLabel(101))
        assertEquals("111 изменений", changesCountLabel(111))
    }
}
