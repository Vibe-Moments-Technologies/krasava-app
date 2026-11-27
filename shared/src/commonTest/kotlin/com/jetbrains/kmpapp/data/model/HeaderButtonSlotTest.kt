package com.jetbrains.kmpapp.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Выбор кнопок шапки (решение владельца): до 3 любых, включая меню.
 * В пузырьке — все НЕ выбранные доступные кнопки.
 */
class HeaderButtonSlotTest {

    private val all = listOf(
        HeaderButton.ADD,
        HeaderButton.USER_LESSON,
        HeaderButton.QR,
        HeaderButton.DAY_BADGE,
        HeaderButton.MENU
    )

    @Test
    fun defaultSelectionIsAddAndMenu() {
        assertEquals(
            listOf(HeaderButton.ADD, HeaderButton.MENU),
            HeaderButton.DEFAULT_SELECTION
        )
    }

    @Test
    fun selectionRoundTripsThroughStorage() {
        val selected = listOf(HeaderButton.QR, HeaderButton.MENU, HeaderButton.ADD)
        val encoded = HeaderButton.encodeSelection(selected)
        assertEquals(
            listOf(HeaderButton.QR, HeaderButton.MENU, HeaderButton.ADD),
            HeaderButton.decodeSelection(encoded)
        )
    }

    @Test
    fun selectionIsLimitedToThree() {
        val encoded = HeaderButton.encodeSelection(all)
        assertEquals(3, encoded.split(',').size)
        assertEquals(3, HeaderButton.decodeSelection(encoded)?.size)
    }

    @Test
    fun legacySingleSlotIsMigrated() {
        assertEquals(HeaderButton.ADD, HeaderButton.fromStoredId(null))
        assertEquals(null, HeaderButton.fromStoredId(HeaderButton.EMPTY_ID))
        assertEquals(HeaderButton.QR, HeaderButton.fromStoredId("qr"))
        assertEquals(null, HeaderButton.fromStoredId("не-кнопка"))
    }

    @Test
    fun menuCanBeTheOnlySelectedButton() {
        val selected = normalizeHeaderSelection(listOf(HeaderButton.MENU), all)
        assertEquals(listOf(HeaderButton.MENU), selected)
    }

    @Test
    fun anyThreeButtonsCanBeSelected() {
        val selected = normalizeHeaderSelection(
            listOf(HeaderButton.ADD, HeaderButton.QR, HeaderButton.MENU), all
        )
        assertEquals(3, selected.size)
        assertTrue(HeaderButton.MENU in selected)
    }

    @Test
    fun noButtonsCanBeSelected() {
        assertTrue(normalizeHeaderSelection(emptyList(), all).isEmpty())
    }

    @Test
    fun unavailableButtonsAreRemovedButOrderKept() {
        val available = listOf(HeaderButton.ADD, HeaderButton.QR, HeaderButton.MENU)
        val selected = normalizeHeaderSelection(
            listOf(HeaderButton.DAY_BADGE, HeaderButton.ADD, HeaderButton.MENU), available
        )
        assertEquals(listOf(HeaderButton.ADD, HeaderButton.MENU), selected)
    }

    @Test
    fun bubbleContainsAllNotSelectedButtons() {
        val selected = listOf(HeaderButton.ADD, HeaderButton.MENU)
        assertEquals(
            listOf(HeaderButton.USER_LESSON, HeaderButton.QR, HeaderButton.DAY_BADGE),
            bubbleHeaderButtons(selected, all)
        )
    }

    @Test
    fun menuIsNeverShownInsideBubble() {
        assertTrue(HeaderButton.MENU !in bubbleHeaderButtons(emptyList(), all))
    }

    @Test
    fun qrIsAlwaysAvailable() {
        assertTrue(HeaderButton.QR in availableHeaderButtons(false, false, false))
    }
}
