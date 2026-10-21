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
 * Контракт данных для виджетов (B1): компактный JSON со ВСЕМИ сохранёнными
 * расписаниями на неделю вперёд. Приложение пишет его в общий контейнер
 * (iOS — App Group), виджет только читает и никогда не ходит в сеть.
 *
 * Почему все расписания, а не одно активное: виджет настраивается пользователем
 * («Изменить виджет» → группа), а список выбора должен быть доступен виджету
 * без обращения к приложению. Параметры хранит система, данные — этот снапшот.
 */

@Serializable
data class WidgetLesson(
    /**
     * Стабильный идентификатор пары из фида. Нужен виджету, чтобы различать
     * две одинаковые пары подряд (ТЗ виджетов п. 6.9: объединять их по
     * названию дисциплины запрещено) и для будущих deep links.
     */
    val id: String,
    val date: String,   // yyyy-MM-dd
    val bell: Int,
    val start: String,  // "09:00" (локальное время)
    val end: String,
    val subject: String,
    val type: String,   // короткий бейдж: ЛК/ПР/ЛАБ/ДР/ДОП
    val room: String,
    val teacher: String,
    /**
     * Учебные группы, участвующие в паре. Нужны виджету для индикатора
     * совместного занятия («2 группы»): по ТЗ считать их количество из числа
     * преподавателей или повторов пар запрещено — только по уникальным
     * идентификаторам групп из фида.
     */
    val groups: List<String> = emptyList()
)

/** Одно расписание: цель (группа/преподаватель/аудитория) и её пары. */
@Serializable
data class WidgetTarget(
    val id: Int,
    val title: String,
    /** Человекочитаемый тип для списка выбора: «Группа», «Преподаватель», «Аудитория». */
    val typeName: String,
    val lessons: List<WidgetLesson> = emptyList()
)

@Serializable
data class WidgetPayload(
    val generatedAt: Long,
    /**
     * Расписание для виджетов iOS 15–16, где выбрать группу нельзя
     * (AppIntent-конфигурация требует iOS 17). Обратная совместимость:
     * берём цель уведомлений, а если её нет — первое сохранённое.
     */
    val defaultTargetId: Int? = null,
    val targets: List<WidgetTarget> = emptyList()
)

object WidgetSnapshotBuilder {
    /** На сколько дней вперёд пишем пары. Неделя — с запасом на «Только сегодня». */
    const val DAYS_AHEAD = 7

    /** null — расписаний нет, писать нечего. */
    fun build(
        targets: List<ScheduleTarget>,
        lessonsByTarget: Map<Int, List<Lesson>>,
        notificationsTargetId: Int?
    ): String? {
        if (targets.isEmpty()) return null
        val today = kotlin.time.Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        val lastDay = today + DatePeriod(days = DAYS_AHEAD - 1)

        val payload = WidgetPayload(
            generatedAt = kotlin.time.Clock.System.now().toEpochMilliseconds(),
            defaultTargetId = notificationsTargetId
                ?.takeIf { id -> targets.any { it.id == id } }
                ?: targets.first().id,
            targets = targets.map { target ->
                WidgetTarget(
                    id = target.id,
                    title = target.targetTitle,
                    typeName = target.type.displayName,
                    lessons = lessonsByTarget[target.id]
                        ?.filter { it.date >= today && it.date <= lastDay }
                        ?.sortedWith(compareBy({ it.date }, { it.bellNumber }, { it.startTime }))
                        .orEmpty()
                        .map { it.toWidgetLesson() }
                )
            }
        )
        return Json.encodeToString(payload)
    }

    private fun Lesson.toWidgetLesson() = WidgetLesson(
        id = id,
        date = date.toString(),
        bell = bellNumber,
        start = startTime,
        end = endTime,
        subject = subject,
        type = lessonType.shortName,
        room = classrooms.joinToString(", "),
        teacher = teachers.joinToString(", "),
        groups = groups
    )
}

/**
 * Точка входа для Swift-стороны: `WidgetBridge.shared.payloadJson()`.
 * Ссылка на репозиторий ставится один раз из общего `App()` — платформенный
 * код не лезет внутрь Koin.
 */
object WidgetBridge {
    private var repository: ScheduleRepository? = null

    fun attach(repository: ScheduleRepository) {
        this.repository = repository
    }

    fun payloadJson(): String? {
        val repo = repository ?: return null
        return try {
            WidgetSnapshotBuilder.build(
                targets = repo.savedTargets.value,
                lessonsByTarget = repo.cachedLessons.value,
                notificationsTargetId = repo.notificationsTargetId.value
            )
        } catch (_: Throwable) {
            null
        }
    }
}