package com.jetbrains.kmpapp.data.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class HiddenLessonsTest {

    private fun lesson(
        id: String,
        type: LessonType = LessonType.LECTURE,
        bell: Int = 1,
        start: String = "09:00"
    ) = Lesson(
        id = id,
        subject = "Математика",
        lessonType = type,
        teachers = emptyList(),
        classrooms = emptyList(),
        bellNumber = bell,
        startTime = start,
        endTime = "10:30",
        date = LocalDate(2026, 11, 9)
    )

    @Test
    fun hiddenLessonsAreRemoved() {
        val lessons = listOf(lesson("remote:1"), lesson("remote:2"))

        assertEquals(
            listOf("remote:2"),
            lessons.withoutHiddenLessons(setOf("remote:1")).map { it.id }
        )
    }

    @Test
    fun emptyHiddenSetKeepsAllLessons() {
        val lessons = listOf(lesson("remote:1"), lesson("remote:2"))

        assertEquals(lessons, lessons.withoutHiddenLessons(emptySet()))
    }

    @Test
    fun sameTimeOrderIsUserThenOriginalThenHidden() {
        val lessons = listOf(
            lesson("remote:hidden"),
            lesson("user:u1:2026-11-09", LessonType.USER),
            lesson("remote:1"),
            lesson("user:u2:2026-11-09", LessonType.USER)
        )
        val hidden = setOf("remote:hidden", "user:u2:2026-11-09")

        assertEquals(
            listOf(
                "user:u1:2026-11-09",
                "remote:1",
                "remote:hidden",
                "user:u2:2026-11-09"
            ),
            lessons.orderedForDisplay(hidden).map { it.id }
        )
    }

    @Test
    fun differentTimesKeepChronologicalOrder() {
        val lessons = listOf(
            lesson("user:u1:2026-11-09", LessonType.USER, bell = 2, start = "10:40"),
            lesson("remote:1")
        )

        assertEquals(
            listOf("remote:1", "user:u1:2026-11-09"),
            lessons.orderedForDisplay(emptySet()).map { it.id }
        )
    }
}
