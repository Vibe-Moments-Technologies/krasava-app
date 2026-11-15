package com.jetbrains.kmpapp.data.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
enum class LessonDiffType {
    ADDED,
    CANCELLED,
    MODIFIED
}

@Serializable
data class LessonDiffItem(
    val type: LessonDiffType,
    val date: LocalDate,
    val bellNumber: Int,
    val subject: String,
    val description: String
)

@Serializable
data class ScheduleDiff(
    val targetId: Int,
    val targetTitle: String,
    val items: List<LessonDiffItem>
)

/**
 * Порог, после которого число изменений в бейдже шапки заменяется на «99+».
 *
 * Репорт владельца: «когда у нас там больше трёх кнопок, она корежится».
 * Причина не только в длине подписи, но и в том, что ширина «Diff (147)»
 * плавает вместе с числом. Ограничиваем разрядность — ширина становится
 * практически постоянной и бейдж перестаёт вытеснять капсулу группы.
 */
private const val BADGE_COUNT_LIMIT = 99

/**
 * Подпись бейджа изменений в шапке расписания: «±3», «±42», «±99+».
 *
 * «Diff (n)» заменён на знак «плюс-минус» и значение (решение владельца):
 * ± читается как «изменения» без латинского слова, а сам бейдж заметно ýже
 * прежнего — в шапке помещается больше кнопок.
 */
fun ScheduleDiff.badgeLabel(): String {
    val total = items.size
    return if (total > BADGE_COUNT_LIMIT) "±$BADGE_COUNT_LIMIT+" else "±$total"
}

/** «1 изменение», «2 изменения», «5 изменений» — для подписи в шторке. */
fun changesCountLabel(count: Int): String {
    val word = when {
        count % 10 == 1 && count % 100 != 11 -> "изменение"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "изменения"
        else -> "изменений"
    }
    return "$count $word"
}
