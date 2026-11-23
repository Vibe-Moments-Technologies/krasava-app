package com.jetbrains.kmpapp.data.model

import com.jetbrains.kmpapp.data.storage.StorageEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Честная статистика хранилища.
 *
 * Раньше экран «Данные и память» показывал константы из вёрстки, а
 * «Настройки» считались по трём ключам из восьмидесяти. Теперь объём
 * считается по фактическим записям — эти тесты фиксируют классификацию
 * и подсчёт, чтобы цифры снова не разъехались с реальностью.
 */
class StorageStatsTest {

    // --- классификация ключей ---

    @Test
    fun scheduleKeysGoToSchedules() {
        assertEquals(StorageCategory.SCHEDULES, storageCategoryOf("krasava_lessons_123"))
        assertEquals(StorageCategory.SCHEDULES, storageCategoryOf("krasava_week_markers_5"))
        assertEquals(StorageCategory.SCHEDULES, storageCategoryOf("krasava_last_sync_7"))
    }

    @Test
    fun targetKeysGoToTargets() {
        assertEquals(StorageCategory.TARGETS, storageCategoryOf("krasava_saved_targets"))
        assertEquals(StorageCategory.TARGETS, storageCategoryOf("krasava_selected_target_id"))
    }

    @Test
    fun userContentIsNotConfusedWithSettings() {
        // Ключ заметок начинается так же, как настройки отображения,
        // но это пользовательский контент — порядок проверок важен.
        assertEquals(StorageCategory.USER_CONTENT, storageCategoryOf("krasava_lesson_notes"))
        assertEquals(StorageCategory.USER_CONTENT, storageCategoryOf("krasava_notes_pages"))
        assertEquals(StorageCategory.USER_CONTENT, storageCategoryOf("krasava_user_lessons"))
        assertEquals(StorageCategory.USER_CONTENT, storageCategoryOf("krasava_hidden_lessons"))
        assertEquals(StorageCategory.SETTINGS, storageCategoryOf("krasava_show_lesson_progress"))
    }

    @Test
    fun tasksAndGamesHaveTheirOwnCategories() {
        assertEquals(StorageCategory.TASKS, storageCategoryOf("saved_study_tasks_list"))
        assertEquals(StorageCategory.TASKS, storageCategoryOf("saved_study_subjects_list"))
        assertEquals(StorageCategory.GAMES, storageCategoryOf("games_record_tetris_best"))
        assertEquals(StorageCategory.GAMES, storageCategoryOf("games_settings_board_vertical"))
    }

    @Test
    fun unknownKeysAreNotLost() {
        // Неизвестный ключ не должен исчезать из подсчёта — иначе итог
        // снова перестанет сходиться с суммой частей.
        assertEquals(StorageCategory.SERVICE, storageCategoryOf("some_future_key"))
    }

    // --- подсчёт объёма ---

    @Test
    fun sizeCountsUtf8BytesNotCharacters() {
        // Русские буквы занимают 2 байта: подсчёт по символам занижал объём.
        val entry = StorageEntry("k", "аб")
        assertEquals(1L + 4L, entry.sizeBytes)
    }

    @Test
    fun totalEqualsSumOfCategories() {
        val entries = listOf(
            StorageEntry("krasava_lessons_1", "abc"),      // 16 + 3
            StorageEntry("krasava_saved_targets", "xy"),   // 20 + 2
            StorageEntry("krasava_app_theme", "DARK"),     // 17 + 4
            StorageEntry("games_record_tetris_best", "1")  // 25 + 1
        )
        val stats = StorageStats.from(entries, 1, 10, 1)
        assertEquals(entries.sumOf { it.sizeBytes }, stats.totalSizeBytes)
        assertEquals(stats.categories.sumOf { it.sizeBytes }, stats.totalSizeBytes)
    }

    @Test
    fun categoriesSortedBySizeDescending() {
        val entries = listOf(
            StorageEntry("krasava_app_theme", "X"),
            StorageEntry("krasava_lessons_1", "X".repeat(500))
        )
        val stats = StorageStats.from(entries, 1, 1, 1)
        assertEquals(StorageCategory.SCHEDULES, stats.categories.first().category)
        assertTrue(stats.categories[0].sizeBytes >= stats.categories[1].sizeBytes)
    }

    @Test
    fun allSettingsKeysAreCountedNotJustThree() {
        //Regression: раньше учитывались только три ключа настроек.
        val settingsKeys = listOf(
            "krasava_app_theme", "krasava_show_empty_lessons", "krasava_selected_target_id",
            "krasava_dock_tabs_order", "krasava_notify_minutes_before",
            "krasava_vpn_warning_enabled", "krasava_qr_button_enabled",
            "krasava_header_slot_button", "krasava_previous_theme"
        )
        val entries = settingsKeys.map { StorageEntry(it, "value") }
        val stats = StorageStats.from(entries, 0, 0, 0)
        // selected_target_id относится к целям, остальные 8 — к настройкам.
        assertEquals(8, stats.usage(StorageCategory.SETTINGS).entriesCount)
        assertEquals(1, stats.usage(StorageCategory.TARGETS).entriesCount)
    }

    @Test
    fun sharesSumToOneAndHandleEmptyStorage() {
        val empty = StorageStats.from(emptyList(), 0, 0, 0)
        assertEquals(0L, empty.totalSizeBytes)
        assertEquals(0f, empty.share(StorageCategory.SCHEDULES))

        val stats = StorageStats.from(
            listOf(
                StorageEntry("krasava_lessons_1", "aaaa"),
                StorageEntry("krasava_app_theme", "bbbb")
            ), 1, 1, 1
        )
        val sum = StorageCategory.entries.sumOf { stats.share(it).toDouble() }
        assertTrue(kotlin.math.abs(sum - 1.0) < 0.001, "доли должны давать 1, было $sum")
    }

    @Test
    fun formatBytesIsHumanReadable() {
        assertEquals("0 Б", StorageStats.formatBytes(0))
        assertEquals("512 Б", StorageStats.formatBytes(512))
        assertEquals("1 КБ", StorageStats.formatBytes(1024))
        assertEquals("1.5 МБ", StorageStats.formatBytes((1.5 * 1024 * 1024).toLong()))
    }
}
