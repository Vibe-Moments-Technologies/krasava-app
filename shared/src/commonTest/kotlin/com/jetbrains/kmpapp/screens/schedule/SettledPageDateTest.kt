package com.jetbrains.kmpapp.screens.schedule

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Арифметика остановившейся страницы расписания.
 *
 * Регрессия, которую ловили пользователи: при переходе currentPage →
 * selectDate во время анимации пейджер запускался заново и гонялся между
 * пятницей и понедельником, уходя в прошлое. Теперь selectDate вызывается
 * только по settledPage. Эти тесты фиксируют само отображение страниц на
 * даты, включая обратный ход через границу недели.
 */
class SettledPageDateTest {

    private val friday = LocalDate(2026, 11, 20) // пятница
    private val basePage = 1000

    @Test
    fun currentFridayKeepsItsPage() {
        val date = dateForSettledPage(friday, basePage, basePage)
        assertEquals(friday, date)
        assertEquals(DayOfWeek.FRIDAY, date.dayOfWeek)
    }

    @Test
    fun nextPageAfterFridayIsFollowingMonday() {
        // Свайп пятница → понедельник: ровно три дня вперёд.
        val date = dateForSettledPage(friday, basePage, basePage + 3)
        assertEquals(LocalDate(2026, 11, 23), date)
        assertEquals(DayOfWeek.MONDAY, date.dayOfWeek)
    }

    @Test
    fun previousPageAfterMondayIsPreviousFriday() {
        // Обратный свайп понедельник → пятница: никаких -1/-2 вместо даты.
        val monday = LocalDate(2026, 11, 23)
        val date = dateForSettledPage(monday, basePage + 3, basePage)
        assertEquals(LocalDate(2026, 11, 20), date)
        assertEquals(DayOfWeek.FRIDAY, date.dayOfWeek)
    }

    @Test
    fun backwardMotionGoesToPreviousWeekWithoutWrappingDayIndex() {
        // Страница назад = неделя назад; индекс дня недели не участвует.
        val date = dateForSettledPage(friday, basePage, basePage - 7)
        assertEquals(LocalDate(2026, 11, 13), date)
        assertEquals(DayOfWeek.FRIDAY, date.dayOfWeek)
    }

    @Test
    fun monthAndYearBoundariesStayCorrect() {
        val newYear = LocalDate(2027, 1, 1)
        assertEquals(
            LocalDate(2026, 12, 31),
            dateForSettledPage(newYear, basePage, basePage - 1)
        )
        assertEquals(
            LocalDate(2027, 1, 4),
            dateForSettledPage(newYear, basePage, basePage + 3)
        )
    }
}
