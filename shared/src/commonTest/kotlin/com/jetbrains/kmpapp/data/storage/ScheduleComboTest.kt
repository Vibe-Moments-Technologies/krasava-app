package com.jetbrains.kmpapp.data.storage

import com.jetbrains.kmpapp.data.model.Lesson
import com.jetbrains.kmpapp.data.model.LessonType
import com.jetbrains.kmpapp.data.model.ScheduleFilter
import com.jetbrains.kmpapp.data.model.ScheduleTarget
import com.jetbrains.kmpapp.data.model.ScheduleTargetType
import com.jetbrains.kmpapp.data.model.forComboSource
import com.jetbrains.kmpapp.data.model.matchesFilter
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScheduleComboTest {

    private fun target(
        id: Int,
        scheduleTarget: Int = ScheduleTargetType.GROUP.id,
        isLocal: Boolean = false,
        isImported: Boolean = false,
        isCombo: Boolean = false,
        comboSourceIds: List<Int> = emptyList()
    ) = ScheduleTarget(
        id = id,
        targetTitle = "Расписание $id",
        fullTitle = "Расписание $id",
        scheduleTarget = scheduleTarget,
        isLocal = isLocal,
        isImported = isImported,
        isCombo = isCombo,
        comboSourceIds = comboSourceIds
    )

    @Test
    fun comboSourcesIgnoreDuplicatesAndCombos() {
        val targets = listOf(target(1), target(2), target(3, isCombo = true))

        assertEquals(
            listOf(1, 2),
            selectComboSourceIds(targets, listOf(1, 1, 2, 3, 99))
        )
    }

    @Test
    fun comboTargetHasComboLabel() {
        val combo = target(-1, isCombo = true, comboSourceIds = listOf(1, 2))

        assertEquals("Комбо", combo.displayTypeName)
    }

    @Test
    fun importedTargetHasImportLabel() {
        val imported = target(-1, isLocal = true, isImported = true)

        assertEquals("Импорт", imported.displayTypeName)
    }

    @Test
    fun filtersSeparateLocalImportedAndCombo() {
        val targets = listOf(
            target(1),
            target(2, scheduleTarget = ScheduleTargetType.TEACHER.id),
            target(-1, isLocal = true),
            target(-2, isLocal = true, isImported = true),
            target(-3, isLocal = true, isCombo = true, comboSourceIds = listOf(1, 2))
        )

        assertEquals(listOf(1), targets.filter { it.matchesFilter(ScheduleFilter.GROUP) }.map { it.id })
        assertEquals(listOf(2), targets.filter { it.matchesFilter(ScheduleFilter.TEACHER) }.map { it.id })
        assertEquals(listOf(-1), targets.filter { it.matchesFilter(ScheduleFilter.OWN) }.map { it.id })
        assertEquals(listOf(-2), targets.filter { it.matchesFilter(ScheduleFilter.IMPORT) }.map { it.id })
        assertEquals(listOf(-3), targets.filter { it.matchesFilter(ScheduleFilter.COMBO) }.map { it.id })
    }

    @Test
    fun removingSourceKeepsComboWithTwoRemaining() {
        val combo = target(-1, isCombo = true, comboSourceIds = listOf(1, 2, 3))

        assertEquals(listOf(2, 3), comboAfterSourceRemoval(combo, 1)?.comboSourceIds)
    }

    @Test
    fun removingSourceDropsComboWithLessThanTwoSources() {
        val combo = target(-1, isCombo = true, comboSourceIds = listOf(1, 2))

        assertNull(comboAfterSourceRemoval(combo, 1))
    }

    @Test
    fun combosUsingSourceFindsOnlyAffectedCombos() {
        val targets = listOf(
            target(1),
            target(-1, isCombo = true, comboSourceIds = listOf(1, 2)),
            target(-2, isCombo = true, comboSourceIds = listOf(2, 3)),
            target(-3, isCombo = true, comboSourceIds = listOf(1, 3))
        )

        assertEquals(
            listOf(-1, -3),
            combosUsingSource(targets, 1).map { it.id }
        )
    }

    @Test
    fun comboLessonIdIsScopedToSource() {
        val lesson = Lesson(
            id = "lesson-1",
            subject = "Математика",
            lessonType = LessonType.LECTURE,
            teachers = emptyList(),
            classrooms = emptyList(),
            bellNumber = 1,
            startTime = "09:00",
            endTime = "10:30",
            date = LocalDate(2026, 11, 26)
        )

        assertEquals("combo:7:lesson-1", lesson.forComboSource(7).id)
    }
}
