package com.jetbrains.kmpapp.theme

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Новая логика тем (решение владельца):
 * 1) выключение темы возвращает на последнюю выбранную, а не на дефолт;
 * 2) сезон Хэллоуина/Нового года СИЛЬНЕЕ явно выбранной темы.
 */
class ThemeReturnLogicTest {

    // --- themeAfterDisable ---

    @Test
    fun disableReturnsToLastChosenTheme() {
        // Выбрал Кавай поверх Сакуры, выключил Кавай → вернулась Сакура.
        assertEquals(
            ThemeOverlay.SAKURA,
            themeAfterDisable(
                turningOff = ThemeOverlay.KAWAII,
                lastChosen = ThemeOverlay.SAKURA
            )
        )
    }

    @Test
    fun disableLastChosenItselfFallsToNone() {
        // Единственная выбранная тема выключена → дефолт.
        assertEquals(
            ThemeOverlay.NONE,
            themeAfterDisable(
                turningOff = ThemeOverlay.SAKURA,
                lastChosen = ThemeOverlay.SAKURA
            )
        )
    }

    @Test
    fun disableWithoutHistoryFallsToNone() {
        assertEquals(
            ThemeOverlay.NONE,
            themeAfterDisable(
                turningOff = ThemeOverlay.MATRIX,
                lastChosen = ThemeOverlay.NONE
            )
        )
    }

    @Test
    fun disableChainRemembersOnlyLatestChoice() {
        // Сакура → Киберпанк → Матрица: выключение Матрицы возвращает
        // на последнюю ВЫБРАННУЮ до неё, а не на весь стек.
        assertEquals(
            ThemeOverlay.CYBERPUNK,
            themeAfterDisable(
                turningOff = ThemeOverlay.MATRIX,
                lastChosen = ThemeOverlay.CYBERPUNK
            )
        )
    }

    // --- effectiveTheme ---

    @Test
    fun seasonOverridesChosenTheme() {
        // В сезон Хэллоуина праздник сильнее выбранной Сакуры.
        assertEquals(
            ThemeOverlay.HALLOWEEN,
            effectiveTheme(
                chosen = ThemeOverlay.SAKURA,
                halloweenSeason = true,
                newYearSeason = false
            )
        )
    }

    @Test
    fun newYearSeasonWinsOverHalloween() {
        // Сезоны не пересекаются, но при ручном включении порядок фиксирован:
        // Новый год приоритетнее (проверка на детерминизм).
        assertEquals(
            ThemeOverlay.NEW_YEAR,
            effectiveTheme(
                chosen = ThemeOverlay.CYBERPUNK,
                halloweenSeason = true,
                newYearSeason = true
            )
        )
    }

    @Test
    fun noSeasonKeepsChosenTheme() {
        assertEquals(
            ThemeOverlay.KAWAII,
            effectiveTheme(
                chosen = ThemeOverlay.KAWAII,
                halloweenSeason = false,
                newYearSeason = false
            )
        )
        assertEquals(
            ThemeOverlay.NONE,
            effectiveTheme(
                chosen = ThemeOverlay.NONE,
                halloweenSeason = false,
                newYearSeason = false
            )
        )
    }

    @Test
    fun chosenHalloweenOutsideSeasonStaysHalloween() {
        // Ручное включение Хэллоуина вне сезона работает как обычный выбор.
        assertEquals(
            ThemeOverlay.HALLOWEEN,
            effectiveTheme(
                chosen = ThemeOverlay.HALLOWEEN,
                halloweenSeason = false,
                newYearSeason = false
            )
        )
    }
}
