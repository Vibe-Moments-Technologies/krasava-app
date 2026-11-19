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
    fun missingKeyMeansFreshInstallDefault() {
        // Ключа нет — свежая установка: в слоте «плюс» (дефолт владельца).
        assertEquals(HeaderButton.ADD, HeaderButton.fromStoredId(null))
        assertEquals(HeaderButton.ADD, HeaderButton.fromStoredId(""))
        assertEquals(HeaderButton.ADD, HeaderButton.fromStoredId("   "))
    }

    @Test
    fun storedEmptyIdMeansAllExtrasOff() {
        // Владелец выключил все допы: слот пуст, в шапке только меню.
        assertNull(HeaderButton.fromStoredId(HeaderButton.EMPTY_ID))
    }

    @Test
    fun corruptedIdDoesNotSilentlyBecomePlus() {
        // Битое значение — не «плюс»: слот пуст, пользователь увидит меню
        // и выберет сам. Молчаливый возврат дефолта прятал бы ошибку.
        assertNull(HeaderButton.fromStoredId("не-кнопка"))
    }

    @Test
    fun storedIdRoundTripWorks() {
        HeaderButton.entries.forEach { button ->
            assertEquals(button, HeaderButton.fromStoredId(HeaderButton.toStoredId(button)))
        }
        // Пустой слот тоже переживает перезапуск.
        assertNull(HeaderButton.fromStoredId(HeaderButton.toStoredId(null)))
        assertEquals(HeaderButton.EMPTY_ID, HeaderButton.toStoredId(null))
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

    // --- содержимое пузырька ---

    @Test
    fun bubbleExcludesButtonAlreadyInSlot() {
        // Владелец: меню содержит ДРУГИЕ кнопки. «Плюс» в слоте — значит
        // в пузырьке его нет (иначе дублирование в двух местах).
        val all = availableHeaderButtons(
            hasTarget = true, userLessonsEnabled = true,
            qrAvailable = true, calendarCollapsed = true
        )
        val slot = resolveHeaderSlot(
            HeaderButton.ADD, hasTarget = true, userLessonsEnabled = true,
            qrAvailable = true, calendarCollapsed = true
        )
        val bubble = all.filter { it != slot }
        assertEquals(3, bubble.size)
        assertTrue(HeaderButton.ADD !in bubble)
        assertTrue(HeaderButton.USER_LESSON in bubble)
    }

    @Test
    fun bubbleHoldsEverythingWhenSlotIsEmpty() {
        // Слот пуст (выбранная кнопка стала недоступна) — в пузырьке всё.
        val all = availableHeaderButtons(
            hasTarget = true, userLessonsEnabled = true,
            qrAvailable = false, calendarCollapsed = false
        )
        val slot = resolveHeaderSlot(
            HeaderButton.QR, hasTarget = true, userLessonsEnabled = true,
            qrAvailable = false, calendarCollapsed = false
        )
        assertNull(slot)
        assertEquals(all, all.filter { it != slot })
    }

    @Test
    fun ownerCanTurnOffAllExtras() {
        // Запрос владельца: «нет возможности выключить вообще все допы».
        // chosen = null — осознанный пустой слот: в шапке только меню,
        // даже когда все кнопки доступны.
        assertNull(
            resolveHeaderSlot(
                chosen = null,
                hasTarget = true, userLessonsEnabled = true,
                qrAvailable = true, calendarCollapsed = true
            )
        )
    }

    @Test
    fun emptySlotPutsEveryAvailableButtonInBubble() {
        val all = availableHeaderButtons(
            hasTarget = true, userLessonsEnabled = true,
            qrAvailable = true, calendarCollapsed = true
        )
        val slot = resolveHeaderSlot(
            chosen = null,
            hasTarget = true, userLessonsEnabled = true,
            qrAvailable = true, calendarCollapsed = true
        )
        assertEquals(all, all.filter { it != slot })
        assertEquals(4, all.filter { it != slot }.size)
    }
}
