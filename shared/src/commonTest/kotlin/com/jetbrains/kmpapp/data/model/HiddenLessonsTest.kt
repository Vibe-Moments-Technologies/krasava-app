package com.jetbrains.kmpapp.data.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class HiddenLessonsTest {

    private fun lesson(id: String) = Lesson(
        id = id,
        subject = "Математика",
        lessonType = LessonType.LECTURE,
        teachers = emptyList(),
        classrooms = emptyList(),
        bellNumber = 1,
        startTime = "09:00",
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
}
