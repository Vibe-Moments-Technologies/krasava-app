package com.jetbrains.kmpapp.data.model

/**
 * Категория данных приложения для экрана «Данные и память».
 *
 * Раньше экран показывал цифры, зашитые в вёрстку («~2.2 МБ» карт,
 * «~64 КБ» задач) — они не имели отношения к реальному содержимому.
 * Теперь каждый ключ хранилища относится к категории, и объём считается
 * по фактическим байтам.
 */
enum class StorageCategory(val title: String) {
    /** Кэш расписаний: пары, маркеры недель, время синхронизации. */
    SCHEDULES("Расписания"),

    /** Сохранённые цели: группы, преподаватели, аудитории. */
    TARGETS("База групп"),

    /** Пользовательский контент: свои пары, скрытые, конспекты. */
    USER_CONTENT("Свои пары и конспекты"),

    /** Задачи и предметы. */
    TASKS("Задачи и дедлайны"),

    /** Настройки интерфейса, темы, уведомления, отладка. */
    SETTINGS("Настройки"),

    /** Рекорды и настройки игр. */
    GAMES("Игры"),

    /** Служебное: кэш аватарок, конфиг, виджет, счётчики. */
    SERVICE("Служебные данные")
}

/**
 * Куда относится ключ хранилища.
 *
 * Порядок проверок важен: более специфичные префиксы идут раньше общих
 * (`krasava_lesson_notes` — контент, а не настройки, хотя начинается так же).
 * Неизвестный ключ попадает в [StorageCategory.SERVICE] — лучше показать
 * его в служебных, чем потерять из подсчёта.
 */
internal fun storageCategoryOf(key: String): StorageCategory = when {
    key.startsWith("krasava_lessons_") ||
        key.startsWith("krasava_week_markers_") ||
        key.startsWith("krasava_last_sync_") -> StorageCategory.SCHEDULES

    key == "krasava_saved_targets" ||
        key == "krasava_selected_target_id" -> StorageCategory.TARGETS

    key == "krasava_user_lessons" ||
        key == "krasava_hidden_lessons" ||
        key == "krasava_notes_pages" ||
        key.startsWith("krasava_lesson_notes") -> StorageCategory.USER_CONTENT

    key.startsWith("saved_study_") -> StorageCategory.TASKS

    key.startsWith("games_") -> StorageCategory.GAMES

    key.startsWith("krasava_") -> StorageCategory.SETTINGS

    else -> StorageCategory.SERVICE
}

/** Объём одной категории с числом записей. */
data class StorageCategoryUsage(
    val category: StorageCategory,
    val sizeBytes: Long,
    val entriesCount: Int
)

/**
 * Реальная статистика хранилища: считается по фактическим парам ключ-значение.
 *
 * [categories] отсортированы по убыванию объёма — крупные категории видны
 * первыми. [totalSizeBytes] — сумма по всем категориям, а не отдельная
 * цифра: раньше итог не сходился с суммой частей.
 */
data class StorageStats(
    val categories: List<StorageCategoryUsage> = emptyList(),
    val schedulesCount: Int = 0,
    val lessonsCount: Int = 0,
    val targetsCount: Int = 0
) {
    val totalSizeBytes: Long get() = categories.sumOf { it.sizeBytes }

    fun usage(category: StorageCategory): StorageCategoryUsage =
        categories.firstOrNull { it.category == category }
            ?: StorageCategoryUsage(category, 0L, 0)

    /** Доля категории в общем объёме (0..1); при пустом хранилище — 0. */
    fun share(category: StorageCategory): Float {
        val total = totalSizeBytes
        if (total <= 0L) return 0f
        return (usage(category).sizeBytes.toFloat() / total).coerceIn(0f, 1f)
    }

    companion object {
        /** Подсчёт по фактическим записям (ключ + значение в UTF-8). */
        fun from(
            entries: List<com.jetbrains.kmpapp.data.storage.StorageEntry>,
            schedulesCount: Int,
            lessonsCount: Int,
            targetsCount: Int
        ): StorageStats {
            val grouped = entries.groupBy { storageCategoryOf(it.key) }
            val usages = grouped.map { (category, items) ->
                StorageCategoryUsage(
                    category = category,
                    sizeBytes = items.sumOf { it.sizeBytes },
                    entriesCount = items.size
                )
            }.sortedByDescending { it.sizeBytes }
            return StorageStats(usages, schedulesCount, lessonsCount, targetsCount)
        }

        fun formatBytes(bytes: Long): String = when {
            bytes < 1024 -> "$bytes Б"
            bytes < 1024 * 1024 -> "${bytes / 1024} КБ"
            else -> {
                val mb = bytes.toDouble() / (1024 * 1024)
                val rounded = (mb * 10).toInt() / 10.0
                "$rounded МБ"
            }
        }
    }

    /** Совместимость с прежним вызовом `storageStats.formatBytes(...)`. */
    fun formatBytes(bytes: Long): String = Companion.formatBytes(bytes)
}