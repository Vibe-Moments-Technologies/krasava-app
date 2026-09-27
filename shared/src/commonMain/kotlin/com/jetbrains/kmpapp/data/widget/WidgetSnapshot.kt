package com.jetbrains.kmpapp.data.widget

import com.jetbrains.kmpapp.data.ScheduleRepository
import com.jetbrains.kmpapp.data.model.Lesson
import com.jetbrains.kmpapp.data.model.ScheduleTarget
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Контракт данных для виджетов (B1): компактный JSON-снапшот «сегодня +
 * завтра» активного расписания. Приложение пишет его в общий контейнер
 * (iOS — App Group), виджет только читает и никогда не ходит в сеть.
 */

@Serializable
data class WidgetLesson(
    val date: String,   // yyyy-MM-dd
    val bell: Int,
    val start: String,  // "09:00" (локальное время)
    val end: String,
    val subject: String,
    val type: String,   // короткий бейдж: ЛК/ПР/ЛАБ/ДР/ДОП
    val room: String,
    val teacher: String
)

@Serializable
data class WidgetSnapshot(
    val generatedAt: Long, // epoch millis UTC
    val targetTitle: String,
    val lessons: List<WidgetLesson>
)

object WidgetSnapshotBuilder {
    /** null — расписание не выбрано, писать/показывать нечего. */
    fun build(target: ScheduleTarget?, lessonsByTarget: Map<Int, List<Lesson>>): String? {
        if (target == null) return null
        val today = kotlin.time.Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        val tomorrow = today + DatePeriod(days = 1)
        val lessons = lessonsByTarget[target.id]
            ?.filter { it.date == today || it.date == tomorrow }
            ?.sortedWith(compareBy({ it.date }, { it.bellNumber }, { it.startTime }))
            .orEmpty()
            .map {
                WidgetLesson(
                    date = it.date.toString(),
                    bell = it.bellNumber,
                    start = it.startTime,
                    end = it.endTime,
                    subject = it.subject,
                    type = it.lessonType.shortName,
                    room = it.classrooms.joinToString(", "),
                    teacher = it.teachers.joinToString(", ")
                )
            }
        val snapshot = WidgetSnapshot(
            generatedAt = kotlin.time.Clock.System.now().toEpochMilliseconds(),
            targetTitle = target.targetTitle,
            lessons = lessons
        )
        return Json.encodeToString(snapshot)
    }
}

/**
 * Точка входа для Swift-стороны: `WidgetBridge.shared.snapshotJson()`.
 * Репозиторий прикрепляется один раз из общего `App()` — без обращения
 * к внутренностям Koin из платформенного кода.
 */
object WidgetBridge {
    private var repository: ScheduleRepository? = null

    fun attach(repository: ScheduleRepository) {
        this.repository = repository
    }

    fun snapshotJson(): String? {
        val repo = repository ?: return null
        return try {
            WidgetSnapshotBuilder.build(repo.selectedTarget.value, repo.cachedLessons.value)
        } catch (_: Throwable) {
            null
        }
    }
}
