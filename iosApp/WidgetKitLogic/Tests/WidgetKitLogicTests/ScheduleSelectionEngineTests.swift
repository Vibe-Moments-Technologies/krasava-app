import XCTest
@testable import WidgetKitLogic

/// Тесты движка выбора актуальной пары.
///
/// Все сценарии из ТЗ (раздел 29) гоняются на заранее подготовленных датах:
/// движок принимает `now` параметром, поэтому системное время не влияет.
/// Фиксированная зона — Europe/Moscow, как и в расписании РТУ МИРЭА.
final class ScheduleSelectionEngineTests: XCTestCase {

    private let tz = TimeZone(identifier: "Europe/Moscow")!
    private var engine: ScheduleSelectionEngine {
        ScheduleSelectionEngine(timeZone: tz)
    }

    // MARK: Фикстуры

    /// «21 октября 2026, 14:30» — момент времени в зоне расписания.
    private func moment(_ text: String) -> Date {
        let f = DateFormatter()
        f.timeZone = tz
        f.locale = Locale(identifier: "ru_RU")
        f.dateFormat = "yyyy-MM-dd HH:mm"
        return f.date(from: text)!
    }

    private func lesson(
        id: String = "l1",
        date: String,
        start: String,
        end: String,
        bell: Int = 2,
        subject: String = "Базы данных",
        type: String = "ПР",
        room: String = "341 (С-20)"
    ) -> WLesson {
        WLesson(
            id: id, date: date, bell: bell, start: start, end: end,
            subject: subject, type: type, room: room, teacher: ""
        )
    }

    private func target(_ lessons: [WLesson], title: String = "БИСО-02-23") -> WTarget {
        WTarget(id: 1, title: title, typeName: "Группа", lessons: lessons)
    }

    /// Расписание из ТЗ, п. 6: пары 21 октября, пусто 22-го, пары 23 октября.
    private var twoDays: WTarget {
        target([
            lesson(id: "a1", date: "2026-10-21", start: "14:20", end: "15:50", bell: 4,
                   subject: "Моделирование информационно-аналитических систем"),
            lesson(id: "a2", date: "2026-10-21", start: "16:20", end: "17:50", bell: 5,
                   subject: "Моделирование информационно-аналитических систем"),
            lesson(id: "b1", date: "2026-10-23", start: "10:40", end: "12:10", bell: 2,
                   subject: "Организационное и правовое обеспечение информационной безопасности",
                   room: "258а (С-20)"),
            lesson(id: "b2", date: "2026-10-23", start: "12:40", end: "14:10", bell: 4,
                   subject: "Организационное и правовое обеспечение информационной безопасности",
                   room: "258а (С-20)")
        ])
    }

    private func state(
        _ target: WTarget?,
        at text: String,
        generatedAt: String? = nil
    ) -> WidgetPresentationState {
        let now = moment(text)
        return engine.makeState(
            target: target,
            now: now,
            generatedAt: generatedAt.map(moment) ?? now
        )
    }

    // MARK: 1–5. Границы занятия

    func testOneMinuteBeforeLessonIsUpcoming() {
        let s = state(twoDays, at: "2026-10-21 14:19")
        XCTAssertEqual(s.primaryLesson?.id, "a1")
        XCTAssertEqual(s.primaryStatus, .soon(minutesUntil: 1))
    }

    func testExactlyAtStartIsCurrent() {
        let s = state(twoDays, at: "2026-10-21 14:20")
        XCTAssertEqual(s.primaryLesson?.id, "a1")
        XCTAssertEqual(s.primaryStatus, .current)
    }

    func testMidLessonIsCurrent() {
        let s = state(twoDays, at: "2026-10-21 15:00")
        XCTAssertEqual(s.primaryLesson?.id, "a1")
        XCTAssertEqual(s.primaryStatus, .current)
    }

    func testOneMinuteBeforeEndIsCurrent() {
        let s = state(twoDays, at: "2026-10-21 15:49")
        XCTAssertEqual(s.primaryLesson?.id, "a1")
        XCTAssertEqual(s.primaryStatus, .current)
    }

    func testExactlyAtEndIsNoLongerCurrent() {
        // Верхняя граница строгая: в момент окончания пара уже не идёт,
        // главной становится следующая (ТЗ п. 7, перемены).
        let s = state(twoDays, at: "2026-10-21 15:50")
        XCTAssertEqual(s.primaryLesson?.id, "a2")
        XCTAssertEqual(s.primaryStatus, .upcoming)
    }

    // MARK: 6. Перемена

    func testBreakBetweenLessonsShowsNext() {
        let s = state(twoDays, at: "2026-10-21 16:00")
        XCTAssertEqual(s.primaryLesson?.id, "a2")
        XCTAssertFalse(s.primaryStatus?.isCurrent ?? true)
        XCTAssertEqual(s.primaryDayContext, .today)
    }

    // MARK: 7. После последней пары дня — ближайший учебный день

    func testAfterLastLessonOfDayMovesToNextStudyDay() {
        // 22 октября занятий нет, поэтому переход сразу на 23 октября.
        let s = state(twoDays, at: "2026-10-21 18:00")
        XCTAssertEqual(s.primaryLesson?.id, "b1", "главной должна стать первая пара 23 октября")
        XCTAssertEqual(s.primaryDayContext, .date("ПТ, 23 ОКТ"))
        XCTAssertEqual(s.primaryDayLabel, "ПТ, 23 ОКТ")
    }

    func testDayWithoutLessonsShowsNearestStudyDayAsTomorrow() {
        let s = state(twoDays, at: "2026-10-22 10:00")
        XCTAssertEqual(s.primaryLesson?.id, "b1")
        XCTAssertEqual(s.primaryDayContext, .tomorrow)
        XCTAssertEqual(s.primaryDayLabel, "ЗАВТРА")
    }

    // MARK: 10. Первый учебный день после выходных

    func testMondayAfterWeekendIsPickedAutomatically() {
        // Пары в пятницу 23-го и в понедельник 26-го; в субботу 24-го
        // главной становится понедельник, а не «завтра».
        let t = target([
            lesson(id: "fri", date: "2026-10-23", start: "10:40", end: "12:10"),
            lesson(id: "mon", date: "2026-10-26", start: "09:00", end: "10:30")
        ])
        let s = state(t, at: "2026-10-24 12:00")
        XCTAssertEqual(s.primaryLesson?.id, "mon")
        XCTAssertEqual(s.primaryDayContext, .date("ПН, 26 ОКТ"))
    }

    // MARK: 11–12. Переход через месяц и год

    func testMonthBoundary() {
        let t = target([
            lesson(id: "oct", date: "2026-10-30", start: "10:40", end: "12:10"),
            lesson(id: "nov", date: "2026-11-02", start: "10:40", end: "12:10")
        ])
        let s = state(t, at: "2026-10-31 12:00")
        XCTAssertEqual(s.primaryLesson?.id, "nov")
        XCTAssertEqual(s.primaryDayContext, .date("ПН, 02 НОЯ"))
    }

    func testYearBoundary() {
        let t = target([
            lesson(id: "dec", date: "2026-12-30", start: "10:40", end: "12:10"),
            lesson(id: "jan", date: "2027-01-11", start: "10:40", end: "12:10")
        ])
        let s = state(t, at: "2026-12-31 12:00")
        XCTAssertEqual(s.primaryLesson?.id, "jan")
        XCTAssertEqual(s.primaryDayContext, .date("ПН, 11 ЯНВ"))
    }

    // MARK: 13–14. Одинаковые пары и разные аудитории

    func testTwoIdenticalLessonsStayDistinct() {
        // ТЗ п. 5: две пары одного предмета подряд — разные записи,
        // объединять их нельзя.
        let s = state(twoDays, at: "2026-10-21 14:30")
        let sameDay = s.upcomingLessons
        XCTAssertEqual(sameDay.count, 1, "вторая пара того же дня должна попасть в список")
        XCTAssertEqual(sameDay.first?.id, "a2")
        XCTAssertEqual(s.primaryLesson?.id, "a1")
    }

    func testDifferentRoomsPerLessonArePreserved() {
        let t = target([
            lesson(id: "r1", date: "2026-10-21", start: "14:20", end: "15:50", room: "341 (С-20)"),
            lesson(id: "r2", date: "2026-10-21", start: "16:20", end: "17:50", room: "375 (С-20)")
        ])
        let s = state(t, at: "2026-10-21 15:00")
        XCTAssertEqual(s.primaryLesson?.roomDisplay, "341 (С-20)")
        XCTAssertEqual(s.upcomingLessons.first?.roomDisplay, "375 (С-20)")
    }

    // MARK: 15. Пересекающиеся занятия

    func testOverlappingLessonsPickFirstOne() {
        // Порядок детерминирован: по началу, затем по номеру пары.
        let t = target([
            lesson(id: "x", date: "2026-10-21", start: "14:20", end: "15:50", bell: 4),
            lesson(id: "y", date: "2026-10-21", start: "14:30", end: "16:00", bell: 5)
        ])
        let s = state(t, at: "2026-10-21 15:00")
        XCTAssertEqual(s.primaryLesson?.id, "x")
        XCTAssertEqual(s.primaryStatus, .current)
    }

    // MARK: 16. Отсутствующая аудитория

    func testMissingRoomShowsPlaceholder() {
        let t = target([lesson(id: "n", date: "2026-10-21", start: "14:20", end: "15:50", room: "")])
        let s = state(t, at: "2026-10-21 15:00")
        XCTAssertEqual(s.primaryLesson?.roomDisplay, "Аудитория уточняется")
        XCTAssertEqual(s.primaryLesson?.roomNumber, "")
        XCTAssertEqual(s.primaryLesson?.buildingLabel, "")
    }

    func testOnlineRoomIsRecognised() {
        let t = target([lesson(id: "o", date: "2026-10-21", start: "14:20", end: "15:50", room: "СДО")])
        let s = state(t, at: "2026-10-21 15:00")
        XCTAssertEqual(s.primaryLesson?.roomDisplay, "Онлайн")
    }

    // MARK: 18. Пустое расписание

    func testEmptyScheduleReportsNoLessons() {
        let s = state(target([]), at: "2026-10-21 12:00")
        XCTAssertTrue(s.isEmpty)
        XCTAssertEqual(s.emptyReason, .noLessons)
    }

    // MARK: 19. Устаревшие данные

    func testStaleSnapshotIsMarked() {
        let s = state(twoDays, at: "2026-10-21 15:00", generatedAt: "2026-10-19 15:00")
        XCTAssertEqual(s.freshness, .stale(hours: 48))
        // Устаревшие данные всё равно показываются — полезную информацию
        // скрывать нельзя (ТЗ п. 7), но с пометкой.
        XCTAssertNotNil(s.primaryLesson)
    }

    func testFreshSnapshotIsNotMarked() {
        let s = state(twoDays, at: "2026-10-21 15:00", generatedAt: "2026-10-21 14:00")
        XCTAssertEqual(s.freshness, .fresh)
    }

    // MARK: Пустая группа и отсутствие данных

    func testNoTargetReportsNoTarget() {
        let s = engine.makeState(target: nil, now: moment("2026-10-21 12:00"), generatedAt: Date())
        XCTAssertTrue(s.isEmpty)
        XCTAssertEqual(s.emptyReason, .noTarget)
    }

    // MARK: Полночь и нестандартная длительность

    func testLessonCrossingMidnightEndsNextDay() {
        let t = target([lesson(id: "night", date: "2026-10-21", start: "23:00", end: "00:30")])
        let s = state(t, at: "2026-10-21 23:30")
        XCTAssertEqual(s.primaryStatus, .current)
    }

    func testNonStandardLessonDuration() {
        // Пара 14:20–16:05 (105 минут) — границы должны соблюдаться.
        let t = target([lesson(id: "long", date: "2026-10-21", start: "14:20", end: "16:05")])
        XCTAssertEqual(state(t, at: "2026-10-21 16:04").primaryStatus, .current)
        XCTAssertEqual(state(t, at: "2026-10-21 16:05").primaryStatus, .upcoming)
    }

    // MARK: Next study day

    func testNextStudyDayIsFilledForDayAfterCurrent() {
        let s = state(twoDays, at: "2026-10-21 15:00")
        XCTAssertEqual(s.nextStudyDay, .date("ПТ, 23 ОКТ"))
        XCTAssertEqual(s.nextStudyDayLessons.count, 2)
    }

    func testNextStudyDayEmptyWhenNothingAhead() {
        let t = target([lesson(id: "only", date: "2026-10-21", start: "14:20", end: "15:50")])
        let s = state(t, at: "2026-10-21 15:00")
        XCTAssertEqual(s.nextStudyDay, .none)
        XCTAssertTrue(s.nextStudyDayLessons.isEmpty)
    }

    // MARK: Аудитория и корпус

    func testRoomIsSplitIntoNumberAndBuilding() {
        let l = lesson(id: "r", date: "2026-10-21", start: "14:20", end: "15:50", room: "258а (С-20)")
        XCTAssertEqual(l.roomNumber, "258а", "буква в номере аудитории должна сохраняться")
        XCTAssertEqual(l.buildingLabel, "С-20")
    }

    func testRoomWithoutBuilding() {
        let l = lesson(id: "r", date: "2026-10-21", start: "14:20", end: "15:50", room: "А-4")
        XCTAssertEqual(l.roomNumber, "А-4")
        XCTAssertEqual(l.buildingLabel, "")
    }
}