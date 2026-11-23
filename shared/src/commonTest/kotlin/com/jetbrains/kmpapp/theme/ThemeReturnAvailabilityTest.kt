package com.jetbrains.kmpapp.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Возврат на предыдущую тему с учётом блокировок.
 *
 * Ветка design_fix ужесточила кофе: она больше не «находится» сама из
 * легаси-ключа активности. Но сохранённая «последняя выбранная» тема могла
 * остаться COFFEE — и возврат на неё вернул бы тему, которую нельзя ни
 * включить, ни выключить. Эти тесты фиксируют починку и то, что логика
 * разблокировки напарника при этом не изменилась.
 */
class ThemeReturnAvailabilityTest {

    private val allQuest = setOf(
        ThemeOverlay.KAWAII, ThemeOverlay.CYBERPUNK,
        ThemeOverlay.MATRIX, ThemeOverlay.COFFEE
    )

    // --- главный баг: заблокированная кофе как цель возврата ---

    @Test
    fun lockedCoffeeIsNotAReturnTarget() {
        // Кофе в «последней выбранной», но квест не пройден → возврат на NONE.
        assertEquals(
            ThemeOverlay.NONE,
            themeReturnTarget(
                turningOff = ThemeOverlay.SAKURA,
                lastChosen = ThemeOverlay.COFFEE,
                unlocked = emptySet(),
                revoked = emptySet()
            )
        )
    }

    @Test
    fun unlockedCoffeeIsAValidReturnTarget() {
        // Квест пройден — логика напарника не сломана: кофе возвращается.
        assertEquals(
            ThemeOverlay.COFFEE,
            themeReturnTarget(
                turningOff = ThemeOverlay.SAKURA,
                lastChosen = ThemeOverlay.COFFEE,
                unlocked = setOf(ThemeOverlay.COFFEE),
                revoked = emptySet()
            )
        )
    }

    @Test
    fun revokedThemeIsNeverAReturnTarget() {
        // Матрица отозвана за нарушение правил — даже если была разблокирована.
        assertEquals(
            ThemeOverlay.NONE,
            themeReturnTarget(
                turningOff = ThemeOverlay.SAKURA,
                lastChosen = ThemeOverlay.MATRIX,
                unlocked = setOf(ThemeOverlay.MATRIX),
                revoked = setOf(ThemeOverlay.MATRIX)
            )
        )
    }

    @Test
    fun everyLockedQuestThemeIsFilteredOut() {
        allQuest.forEach { quest ->
            assertEquals(
                ThemeOverlay.NONE,
                themeReturnTarget(
                    turningOff = ThemeOverlay.SAKURA,
                    lastChosen = quest,
                    unlocked = emptySet(),
                    revoked = emptySet()
                ),
                "заблокированная $quest не должна быть целью возврата"
            )
        }
    }

    @Test
    fun unlockedQuestThemesStillReturn() {
        allQuest.forEach { quest ->
            assertEquals(
                quest,
                themeReturnTarget(
                    turningOff = ThemeOverlay.SAKURA,
                    lastChosen = quest,
                    unlocked = allQuest,
                    revoked = emptySet()
                ),
                "найденная $quest должна возвращаться как раньше"
            )
        }
    }

    // --- темы без квеста не пострадали ---

    @Test
    fun sakuraAlwaysReturnsRegardlessOfUnlocks() {
        assertEquals(
            ThemeOverlay.SAKURA,
            themeReturnTarget(
                turningOff = ThemeOverlay.COFFEE,
                lastChosen = ThemeOverlay.SAKURA,
                unlocked = emptySet(),
                revoked = emptySet()
            )
        )
    }

    @Test
    fun seasonalThemesAreNotQuestGated() {
        // Сезонные включаются правилом, а не квестом: возврат на них допустим.
        listOf(ThemeOverlay.HALLOWEEN, ThemeOverlay.NEW_YEAR).forEach { seasonal ->
            assertEquals(
                seasonal,
                themeReturnTarget(
                    turningOff = ThemeOverlay.SAKURA,
                    lastChosen = seasonal,
                    unlocked = emptySet(),
                    revoked = emptySet()
                )
            )
        }
    }

    @Test
    fun turningOffLastChosenItselfStillFallsToNone() {
        // Прежнее правило сохранено: выключаем то, что и было последним.
        assertEquals(
            ThemeOverlay.NONE,
            themeReturnTarget(
                turningOff = ThemeOverlay.COFFEE,
                lastChosen = ThemeOverlay.COFFEE,
                unlocked = setOf(ThemeOverlay.COFFEE),
                revoked = emptySet()
            )
        )
    }

    // --- isThemeAvailable: одно правило на загрузку и на возврат ---

    @Test
    fun availabilityMatchesQuestGating() {
        assertFalse(isThemeAvailable(ThemeOverlay.COFFEE, emptySet(), emptySet()))
        assertTrue(isThemeAvailable(ThemeOverlay.COFFEE, setOf(ThemeOverlay.COFFEE), emptySet()))
        assertTrue(isThemeAvailable(ThemeOverlay.SAKURA, emptySet(), emptySet()))
        assertTrue(isThemeAvailable(ThemeOverlay.NONE, emptySet(), emptySet()))
        assertFalse(
            isThemeAvailable(ThemeOverlay.MATRIX, setOf(ThemeOverlay.MATRIX), setOf(ThemeOverlay.MATRIX))
        )
    }
}
