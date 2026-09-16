package com.jetbrains.kmpapp.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class DateUtilsTest {

    // Осенний семестр 2026: 1 сентября — вторник. Первая (неполная) неделя
    // всё равно должна считаться первой, а текущая — третьей.
    @Test
    fun weekNumberCountsPartialFirstWeek() {
        assertEquals(1, DateUtils.getWeekInfo(LocalDate(2026, 9, 1)).weekNumber)
        assertEquals(2, DateUtils.getWeekInfo(LocalDate(2026, 9, 7)).weekNumber)
        assertEquals(3, DateUtils.getWeekInfo(LocalDate(2026, 9, 16)).weekNumber)
    }

    @Test
    fun springSemesterStartsOnMonday() {
        assertEquals(1, DateUtils.getWeekInfo(LocalDate(2026, 2, 9)).weekNumber)
        assertEquals(2, DateUtils.getWeekInfo(LocalDate(2026, 2, 16)).weekNumber)
    }

    @Test
    fun parityFollowsWeekNumber() {
        assertTrue(DateUtils.getWeekInfo(LocalDate(2026, 9, 7)).isEven)
        assertFalse(DateUtils.getWeekInfo(LocalDate(2026, 9, 16)).isEven)
    }
}
