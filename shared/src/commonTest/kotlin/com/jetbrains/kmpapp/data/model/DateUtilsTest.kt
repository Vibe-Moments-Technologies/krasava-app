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

    @Test
    fun feedMarkersTakePrecedenceOverCalculation() {
        SemesterWeeks.set(listOf(WeekMarker(5, LocalDate(2026, 9, 14))))
        try {
            assertEquals(5, DateUtils.getWeekInfo(LocalDate(2026, 9, 16)).weekNumber)
        } finally {
            SemesterWeeks.set(emptyList())
        }
        // фолбэк снова работает
        assertEquals(3, DateUtils.getWeekInfo(LocalDate(2026, 9, 16)).weekNumber)
    }

    // Осень 2026: маркер «1 неделя» в фиде начинается со вторника 01.09,
    // но понедельник 31.08 той же недели тоже должен давать неделю 1.
    @Test
    fun markerStartingMidWeekCoversWholeWeek() {
        SemesterWeeks.set(
            listOf(
                WeekMarker(1, LocalDate(2026, 9, 1)),
                WeekMarker(2, LocalDate(2026, 9, 7))
            )
        )
        try {
            assertEquals(1, DateUtils.getWeekInfo(LocalDate(2026, 8, 31)).weekNumber)
            assertEquals(1, DateUtils.getWeekInfo(LocalDate(2026, 9, 1)).weekNumber)
            assertEquals(2, DateUtils.getWeekInfo(LocalDate(2026, 9, 7)).weekNumber)
        } finally {
            SemesterWeeks.set(emptyList())
        }
    }

    // Сезон хэллоуинской темы: 28 октября — 3 ноября включительно
    // (±3 дня от 31 октября). Границы проверяем с обеих сторон: окно
    // переходит через границу месяцев, поэтому «31 + 3» нельзя считать
    // арифметикой дней месяца.
    @Test
    fun halloweenSeasonBoundaries() {
        // До сезона.
        assertFalse(DateUtils.isHalloweenSeason(LocalDate(2026, 10, 27)))
        // Первый день сезона.
        assertTrue(DateUtils.isHalloweenSeason(LocalDate(2026, 10, 28)))
        // Сам праздник.
        assertTrue(DateUtils.isHalloweenSeason(LocalDate(2026, 10, 31)))
        // Переход через границу месяцев внутри сезона.
        assertTrue(DateUtils.isHalloweenSeason(LocalDate(2026, 11, 1)))
        // Последний день сезона.
        assertTrue(DateUtils.isHalloweenSeason(LocalDate(2026, 11, 3)))
        // После сезона.
        assertFalse(DateUtils.isHalloweenSeason(LocalDate(2026, 11, 4)))
    }

    @Test
    fun halloweenSeasonIsYearLocal() {
        // В другие годы окно то же: конец октября — начало ноября.
        assertTrue(DateUtils.isHalloweenSeason(LocalDate(2027, 10, 29)))
        assertFalse(DateUtils.isHalloweenSeason(LocalDate(2027, 10, 26)))
        // Летом тема выключена.
        assertFalse(DateUtils.isHalloweenSeason(LocalDate(2026, 7, 31)))
    }

    // Новогодний сезон: 30 декабря — 11 января включительно. Окно переходит
    // через границу года, поэтому проверяем обе половины и дни вокруг.
    @Test
    fun newYearSeasonBoundaries() {
        assertFalse(DateUtils.isNewYearSeason(LocalDate(2026, 12, 29)))
        assertTrue(DateUtils.isNewYearSeason(LocalDate(2026, 12, 30)))
        assertTrue(DateUtils.isNewYearSeason(LocalDate(2026, 12, 31)))
        assertTrue(DateUtils.isNewYearSeason(LocalDate(2027, 1, 1)))
        assertTrue(DateUtils.isNewYearSeason(LocalDate(2027, 1, 11)))
        assertFalse(DateUtils.isNewYearSeason(LocalDate(2027, 1, 12)))
        assertFalse(DateUtils.isNewYearSeason(LocalDate(2027, 6, 1)))
    }

    @Test
    fun newYearSeasonYearKeepsDecemberAndJanuaryTogether() {
        // Отказ 30 декабря должен действовать и в январе следующего года.
        assertEquals(2027, DateUtils.newYearSeasonYear(LocalDate(2026, 12, 30)))
        assertEquals(2027, DateUtils.newYearSeasonYear(LocalDate(2027, 1, 11)))
        // Следующий декабрь — уже новый сезон.
        assertEquals(2028, DateUtils.newYearSeasonYear(LocalDate(2027, 12, 30)))
    }
}
