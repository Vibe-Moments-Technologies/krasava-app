package com.jetbrains.kmpapp.data.share

import com.jetbrains.kmpapp.data.model.UserLesson
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScheduleShareCodecTest {

    private val schedule = SharedSchedule(
        name = "Тест (п)",
        lessons = listOf(
            UserLesson(
                id = "rule-1",
                subject = "Математический анализ",
                date = LocalDate(2026, 2, 9),
                bellNumber = 2,
                teacher = "Иванов И.И.",
                auditorium = "А-101",
                groups = "ИКБО-01-24",
                repeatEven = true,
                excludedDates = listOf(LocalDate(2026, 2, 23))
            ),
            UserLesson(
                id = "rule-2",
                subject = "Физика",
                date = LocalDate(2026, 2, 10),
                bellNumber = 4
            )
        )
    )

    @Test
    fun roundTripPreservesSchedule() {
        val link = ScheduleShareCodec.encode(schedule)

        assertTrue(link.startsWith(ScheduleShareCodec.LINK_PREFIX))
        assertEquals(schedule, ScheduleShareCodec.decode(link).getOrThrow())
    }

    @Test
    fun acceptsRawPrefixAndLineWraps() {
        val link = ScheduleShareCodec.encode(schedule)
        val raw = link.removePrefix(ScheduleShareCodec.LINK_PREFIX)
        val wrapped = raw.chunked(24).joinToString("\n")

        assertEquals(schedule, ScheduleShareCodec.decode("krasava/my/$wrapped").getOrThrow())
    }

    @Test
    fun rejectsDamagedPayload() {
        assertTrue(ScheduleShareCodec.decode("krasava://my/not-base64!").isFailure)
        assertTrue(ScheduleShareCodec.decode("https://example.com/abc").isFailure)
    }

    @Test
    fun rejectsUnknownVersion() {
        val link = ScheduleShareCodec.encode(schedule.copy(version = 99))

        assertTrue(ScheduleShareCodec.decode(link).isFailure)
    }

    @Test
    fun rejectsBlankName() {
        val link = ScheduleShareCodec.encode(schedule.copy(name = "   "))

        assertTrue(ScheduleShareCodec.decode(link).isFailure)
    }
}
