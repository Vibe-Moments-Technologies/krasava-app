package com.jetbrains.kmpapp.data.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.random.Random
import kotlin.time.Clock

private const val USER_LESSON_ID_PREFIX = "user"

/** Что именно удалить в меню «Безвозвратно удалить» у пользовательской пары. */
enum class UserLessonDeleteScope {
    /** Только это появление пары (конкретная дата). */
    THIS,

    /** Все появления этой пары в нечётные недели. */
    ODD_WEEKS,

    /** Все появления этой пары в чётные недели. */
    EVEN_WEEKS,

    /** Все появления этой пары (само правило целиком). */
    ALL
}

/**
 * Пользовательская («своя») пара. Это не запись из фида МИРЭА, а правило,
 * которое живёт только в приложении и привязано к конкретному сохранённому
 * расписанию.
 *
 * [date] — первое (базовое) появление пары. [repeatEven]/[repeatOdd] включают
 * повтор на все чётные/нечётные недели в тот же день недели. [excludedDates]
 * хранит точечные исключения: «удалить только эту пару» не должно сносить
 * всё правило.
 */
@Serializable
data class UserLesson(
    val id: String,
    val subject: String,
    val date: LocalDate,
    val bellNumber: Int,
    val teacher: String = "",
    val auditorium: String = "",
    val groups: String = "",
    val repeatEven: Boolean = false,
    val repeatOdd: Boolean = false,
    val excludedDates: List<LocalDate> = emptyList()
) {
    /** Появляется ли пара в указанный день. */
    fun occursOn(day: LocalDate): Boolean {
        if (day in excludedDates) return false
        if (day == date) return true
        if (day.dayOfWeek != date.dayOfWeek) return false
        val isEven = DateUtils.getWeekInfo(day).isEven
        return if (isEven) repeatEven else repeatOdd
    }

    /** Останется ли у правила хотя бы одно появление после правок. */
    fun hasOccurrences(): Boolean =
        date !in excludedDates || repeatEven || repeatOdd

    /** Удалить только конкретное появление (дату), не трогая остальные. */
    fun withoutOccurrence(day: LocalDate): UserLesson =
        copy(excludedDates = (excludedDates + day).distinct())

    /**
     * Убрать все появления в чётные или нечётные недели.
     *
     * Если базовый день попадает в удаляемую чётность — он тоже исключается,
     * иначе «удалить все чётные» оставило бы его на месте.
     */
    fun withoutWeekParity(isEven: Boolean): UserLesson {
        val withoutFlag = if (isEven) copy(repeatEven = false) else copy(repeatOdd = false)
        return if (DateUtils.getWeekInfo(date).isEven == isEven) {
            withoutFlag.copy(excludedDates = (withoutFlag.excludedDates + date).distinct())
        } else {
            withoutFlag
        }
    }

    /** Превращает правило в обычную карточку пары для конкретного дня. */
    fun toLesson(day: LocalDate): Lesson {
        val bell = defaultBells.firstOrNull { it.number == bellNumber }
        return Lesson(
            id = "$USER_LESSON_ID_PREFIX:$id:$day",
            subject = subject,
            lessonType = LessonType.USER,
            teachers = splitValues(teacher),
            classrooms = splitValues(auditorium),
            bellNumber = bellNumber,
            startTime = bell?.startTime ?: "—",
            endTime = bell?.endTime ?: "—",
            date = day,
            groups = splitValues(groups)
        )
    }
}

/** Ссылка на исходное правило из id карточки [Lesson]. */
data class UserLessonRef(
    val ruleId: String,
    val date: LocalDate
)

/** Разбирает id карточки пользовательской пары; для остальных — null. */
fun parseUserLessonRef(lessonId: String): UserLessonRef? {
    val parts = lessonId.split(':', limit = 3)
    if (parts.size != 3 || parts[0] != USER_LESSON_ID_PREFIX) return null
    val date = runCatching { LocalDate.parse(parts[2]) }.getOrNull() ?: return null
    return UserLessonRef(ruleId = parts[1], date = date)
}

/** Уникальный id правила: время + случайный хвост от совпадений в одну мс. */
fun newUserLessonId(): String =
    "${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt(100_000, 999_999)}"

private fun splitValues(raw: String): List<String> =
    raw.split(',', ';').map { it.trim() }.filter { it.isNotEmpty() }
