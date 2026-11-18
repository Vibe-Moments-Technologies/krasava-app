package com.jetbrains.kmpapp.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Кнопки шапки расписания (решение владельца): слот ОДИН рядом с меню,
 * остальные кнопки живут в пузырьке над лентой календаря.
 */
class HeaderButtonSlotTest {

    @Test
    fun defaultSlotIsAddButton() {
        assertEquals(HeaderButton.ADD, HeaderButton.DEFAULT)
    }

    @Test
    fun unknownIdFallsBackToDefault() {
        assertEquals(HeaderButton.ADD, HeaderButton.fromId(null))
        assertEquals(HeaderButton.ADD, HeaderButton.fromId("не-кнопка"))
        assertEquals(HeaderButton.ADD, HeaderButton.fromId(""))
    }

    @Test
    fun idRoundTripWorks() {
        HeaderButton.entries.forEach { button ->
            assertEquals(button, HeaderButton.fromId(button.id))
        }
    }

    // --- доступные кнопки (пузырёк меню) ---

    @Test
    fun withoutTargetOnlyTargetBoundButtonsAreMissing() {
        // QR не привязан к расписанию: он открывает раздел сканера и доступен
        // даже без выбранной группы (поведение сохранено как было).
        assertEquals(
            listOf(HeaderButton.ADD, HeaderButton.QR),
            availableHeaderButtons(
                hasTarget = false,
                userLessonsEnabled = true,
                qrAvailable = true,
                calendarCollapsed = true
            )
        )
        // Без расписания и без настройки QR остаётся только «плюс».
        assertEquals(
            listOf(HeaderButton.ADD),
            availableHeaderButtons(
                hasTarget = false,
                userLessonsEnabled = true,
                qrAvailable = false,
                calendarCollapsed = true
            )
        )
    }

    @Test
    fun userLessonNeedsTargetAndSetting() {
        val base = availableHeaderButtons(
            hasTarget = true, userLessonsEnabled = false,
            qrAvailable = false, calendarCollapsed = false
        )
        assertTrue(HeaderButton.USER_LESSON !in base)

        val withSetting = availableHeaderButtons(
            hasTarget = true, userLessonsEnabled = true,
            qrAvailable = false, calendarCollapsed = false
        )
        assertTrue(HeaderButton.USER_LESSON in withSetting)
    }

    @Test
    fun dayBadgeOnlyWhenCalendarCollapsed() {
        val expanded = availableHeaderButtons(
            hasTarget = true, userLessonsEnabled = true,
            qrAvailable = true, calendarCollapsed = false
        )
        assertTrue(HeaderButton.DAY_BADGE !in expanded)

        val collapsed = availableHeaderButtons(
            hasTarget = true, userLessonsEnabled = true,
            qrAvailable = true, calendarCollapsed = true
        )
        assertTrue(HeaderButton.DAY_BADGE in collapsed)
    }

    @Test
    fun fullSetHasFourButtons() {
        val all = availableHeaderButtons(
            hasTarget = true, userLessonsEnabled = true,
            qrAvailable = true, calendarCollapsed = true
        )
        assertEquals(4, all.size)
        assertEquals(HeaderButton.entries.size, all.size)
    }

    // --- слот ---

    @Test
    fun slotKeepsChosenButtonWhenAvailable() {
        assertEquals(
            HeaderButton.QR,
            resolveHeaderSlot(
                chosen = HeaderButton.QR,
                hasTarget = true, userLessonsEnabled = true,
                qrAvailable = true, calendarCollapsed = true
            )
        )
    }

    @Test
    fun slotEmptiesWhenChosenButtonBecomesUnavailable() {
        // QR выбран, но настройку выключили → в шапке только меню.
        assertNull(
            resolveHeaderSlot(
                chosen = HeaderButton.QR,
                hasTarget = true, userLessonsEnabled = true,
                qrAvailable = false, calendarCollapsed = true
            )
        )
    }

    @Test
    fun dayBadgeSlotEmptiesWhenCalendarExpanded() {
        assertNull(
            resolveHeaderSlot(
                chosen = HeaderButton.DAY_BADGE,
                hasTarget = true, userLessonsEnabled = true,
                qrAvailable = true, calendarCollapsed = false
            )
        )
    }

    @Test
    fun addSlotAlwaysAvailable() {
        assertEquals(
            HeaderButton.ADD,
            resolveHeaderSlot(
                chosen = HeaderButton.ADD,
                hasTarget = false, userLessonsEnabled = false,
                qrAvailable = false, calendarCollapsed = false
            )
        )
    }
}
