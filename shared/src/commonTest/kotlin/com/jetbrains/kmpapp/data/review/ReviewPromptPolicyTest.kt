package com.jetbrains.kmpapp.data.review

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Просьба о звезде/отзыве (запрос владельца: «заходит 7 раз — на 8-й
 * открывается предложение») и определение источника установки.
 */
class ReviewPromptPolicyTest {

    // --- когда показывать ---

    @Test
    fun notShownBeforeEighthLaunch() {
        for (launch in 1..7) {
            assertFalse(
                ReviewPromptPolicy.shouldShow(launch, resolved = false, snoozeUntilLaunch = null, snoozeCount = 0),
                "не должно показываться на запуске $launch"
            )
        }
    }

    @Test
    fun shownOnEighthLaunch() {
        assertTrue(
            ReviewPromptPolicy.shouldShow(8, resolved = false, snoozeUntilLaunch = null, snoozeCount = 0)
        )
    }

    @Test
    fun stillShownLaterIfUserIgnoredIt() {
        // Диалог закрыли тапом мимо (не ответом) — на следующих запусках
        // просьба остаётся доступной, пока не будет ответа или откладываний.
        assertTrue(
            ReviewPromptPolicy.shouldShow(9, resolved = false, snoozeUntilLaunch = null, snoozeCount = 0)
        )
        assertTrue(
            ReviewPromptPolicy.shouldShow(50, resolved = false, snoozeUntilLaunch = null, snoozeCount = 0)
        )
    }

    @Test
    fun resolvedNeverShowsAgain() {
        assertFalse(
            ReviewPromptPolicy.shouldShow(8, resolved = true, snoozeUntilLaunch = null, snoozeCount = 0)
        )
        assertFalse(
            ReviewPromptPolicy.shouldShow(100, resolved = true, snoozeUntilLaunch = null, snoozeCount = 0)
        )
    }

    // --- откладывание ---

    @Test
    fun snoozeWaitsConfiguredLaunches() {
        val until = ReviewPromptPolicy.snoozeUntil(8)
        assertEquals(8 + ReviewPromptPolicy.SNOOZE_LAUNCHES, until)
        assertFalse(
            ReviewPromptPolicy.shouldShow(9, resolved = false, snoozeUntilLaunch = until, snoozeCount = 1)
        )
        assertFalse(
            ReviewPromptPolicy.shouldShow(until - 1, resolved = false, snoozeUntilLaunch = until, snoozeCount = 1)
        )
        assertTrue(
            ReviewPromptPolicy.shouldShow(until, resolved = false, snoozeUntilLaunch = until, snoozeCount = 1)
        )
    }

    @Test
    fun snoozeLimitStopsPromptForever() {
        // Откладывания исчерпаны — просьба больше не показывается даже
        // спустя много запусков (иначе «позже» = вечный показ).
        assertFalse(
            ReviewPromptPolicy.shouldShow(
                launchCount = 500,
                resolved = false,
                snoozeUntilLaunch = 10,
                snoozeCount = ReviewPromptPolicy.MAX_SNOOZES
            )
        )
    }

    // --- источник установки ---

    @Test
    fun ruStoreInstallerRecognized() {
        assertEquals(InstallSource.RU_STORE, installSourceFromInstallerPackage("ru.vcmob.store"))
        assertEquals(InstallSource.RU_STORE, installSourceFromInstallerPackage("ru.rustore"))
        assertEquals(InstallSource.RU_STORE, installSourceFromInstallerPackage("ru.vk.store"))
        // Запас на ребрендинг пакета: любая строка с rustore.
        assertEquals(InstallSource.RU_STORE, installSourceFromInstallerPackage("ru.rustore.beta"))
        assertEquals(InstallSource.RU_STORE, installSourceFromInstallerPackage("RuStore"))
    }

    @Test
    fun googlePlayRecognizedButHasNoListing() {
        assertEquals(InstallSource.GOOGLE_PLAY, installSourceFromInstallerPackage("com.android.vending"))
        // Приложение в Google Play не публиковалось — отзыв там не оставить.
        assertFalse(InstallSource.GOOGLE_PLAY.hasStoreListing)
    }

    @Test
    fun unknownAndMissingInstallerIsSideload() {
        assertEquals(InstallSource.SIDELOAD, installSourceFromInstallerPackage(null))
        assertEquals(InstallSource.SIDELOAD, installSourceFromInstallerPackage(""))
        assertEquals(InstallSource.SIDELOAD, installSourceFromInstallerPackage("  "))
        assertEquals(InstallSource.SIDELOAD, installSourceFromInstallerPackage("com.some.filemanager"))
    }

    @Test
    fun onlyRuStoreAllowsAskingForReview() {
        assertTrue(InstallSource.RU_STORE.hasStoreListing)
        assertFalse(InstallSource.APP_STORE.hasStoreListing)
        assertFalse(InstallSource.SIDELOAD.hasStoreListing)
    }
}
