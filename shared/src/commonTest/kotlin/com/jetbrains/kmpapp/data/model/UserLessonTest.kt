package com.jetbrains.kmpapp.data.model

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserLessonTest {

    private val monday = LocalDate(2025, 1, 6)

    private fun sameWeekdayWithParity(after: LocalDate, even: Boolean): LocalDate {
        var day = after.plus(DatePeriod(days = 7))
        while (DateUtils.getWeekInfo(day).isEven != even) {
            day = day.plus(DatePeriod(days = 7))
        }
        return day
    }

    private fun rule(
        repeatEven: Boolean = false,
        repeatOdd: Boolean = false,
        excludedDates: List<LocalDate> = emptyList()
    ) = UserLesson(
        id = "rule-1",
        subject = "Консультация",
        date = monday,
        bellNumber = 3,
        teacher = "Иванов И.И.",
        auditorium = "А-101",
        groups = "ИКБО-01-24, ИКБО-02-24",
        repeatEven = repeatEven,
        repeatOdd = repeatOdd,
        excludedDates = excludedDates
    )

    @Test
    fun baseDateOccursEvenWithoutRepeats() {
        val lesson = rule()
        assertTrue(lesson.occursOn(monday))
        assertFalse(lesson.occursOn(monday.plus(DatePeriod(days = 7))))
        assertFalse(lesson.occursOn(monday.plus(DatePeriod(days = 1))))
    }

    @Test
    fun excludedDateDoesNotOccur() {
        val lesson = rule(excludedDates = listOf(monday))
        assertFalse(lesson.occursOn(monday))
    }

    @Test
    fun repeatEvenShowsOnlyEvenWeeks() {
        val lesson = rule(repeatEven = true)
        val evenDay = sameWeekdayWithParity(monday, even = true)
        val oddDay = sameWeekdayWithParity(monday, even = false)

        assertTrue(lesson.occursOn(evenDay))
        assertFalse(lesson.occursOn(oddDay))
    }

    @Test
    fun repeatOddShowsOnlyOddWeeks() {
        val lesson = rule(repeatOdd = true)
        val evenDay = sameWeekdayWithParity(monday, even = true)
        val oddDay = sameWeekdayWithParity(monday, even = false)

        assertTrue(lesson.occursOn(oddDay))
        assertFalse(lesson.occursOn(evenDay))
    }

    @Test
    fun bothRepeatsShowEveryWeek() {
        val lesson = rule(repeatEven = true, repeatOdd = true)
        assertTrue(lesson.occursOn(sameWeekdayWithParity(monday, even = true)))
        assertTrue(lesson.occursOn(sameWeekdayWithParity(monday, even = false)))
    }

    @Test
    fun withoutOccurrenceKeepsOtherDates() {
        val lesson = rule(repeatEven = true, repeatOdd = true)
        val nextWeek = monday.plus(DatePeriod(days = 7))
        val updated = lesson.withoutOccurrence(nextWeek)

        assertTrue(updated.occursOn(monday))
        assertFalse(updated.occursOn(nextWeek))
    }

    @Test
    fun withoutWeekParityRemovesFlagAndBaseDateOfThatParity() {
        val baseIsEven = DateUtils.getWeekInfo(monday).isEven
        val lesson = rule(repeatEven = true, repeatOdd = true)
        val updated = lesson.withoutWeekParity(baseIsEven)

        assertFalse(updated.occursOn(monday))
        if (baseIsEven) {
            assertFalse(updated.repeatEven)
            assertTrue(updated.repeatOdd)
        } else {
            assertTrue(updated.repeatEven)
            assertFalse(updated.repeatOdd)
        }
    }

    @Test
    fun withoutWeekParityKeepsBaseFromOtherParity() {
        val baseIsEven = DateUtils.getWeekInfo(monday).isEven
        val lesson = rule(repeatEven = true, repeatOdd = true)
        val updated = lesson.withoutWeekParity(!baseIsEven)

        assertTrue(updated.occursOn(monday))
    }

    @Test
    fun hasOccurrencesFalseWhenSingleDateExcluded() {
        val lesson = rule(excludedDates = listOf(monday))
        assertFalse(lesson.hasOccurrences())
        assertTrue(rule().hasOccurrences())
        assertTrue(rule(repeatEven = true, excludedDates = listOf(monday)).hasOccurrences())
    }

    @Test
    fun toLessonMapsFieldsAndBuildsParseableId() {
        val evenDay = sameWeekdayWithParity(monday, even = true)
        val lesson = rule(repeatEven = true).toLesson(evenDay)

        assertEquals(LessonType.USER, lesson.lessonType)
        assertEquals("Консультация", lesson.subject)
        assertEquals(3, lesson.bellNumber)
        assertEquals(evenDay, lesson.date)
        assertEquals(listOf("Иванов И.И."), lesson.teachers)
        assertEquals(listOf("А-101"), lesson.classrooms)
        assertEquals(listOf("ИКБО-01-24", "ИКБО-02-24"), lesson.groups)

        val ref = assertNotNull(parseUserLessonRef(lesson.id))
        assertEquals("rule-1", ref.ruleId)
        assertEquals(evenDay, ref.date)
    }

    @Test
    fun parseUserLessonRefRejectsOtherLessons() {
        assertNull(parseUserLessonRef("feed-lesson-42"))
        assertNull(parseUserLessonRef("user:rule-1"))
        assertNull(parseUserLessonRef("user:rule-1:not-a-date"))
        assertNull(parseUserLessonRef("other:rule-1:2025-01-06"))
    }
}
