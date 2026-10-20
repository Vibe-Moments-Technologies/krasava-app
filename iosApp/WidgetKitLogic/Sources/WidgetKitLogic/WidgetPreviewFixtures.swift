import Foundation

// MARK: - Фикстуры для Preview и placeholder
//
// ТЗ п. 30: каждое представление проверяется на наборе состояний — CURRENT,
// UPCOMING, SOON, FUTURE_DAY, NO_SCHEDULE, DATA_UNAVAILABLE, LONG_TITLE,
// ONLY_ONE_LESSON, MANY_LESSONS. Фикстуры строятся тем же движком, что и
// реальные данные, поэтому Preview показывает настоящую логику, а не
// нарисованную вручную разметку.
//
// Файл без SwiftUI: участвует в unit-тестах фикстур и в placeholder.

/// Готовый набор для Preview: состояние + подпись сценария.
struct WidgetPreviewFixture {
    let name: String
    let targetId: Int?
    let state: WidgetPresentationState
}

enum WidgetPreviewFixtures {

    // MARK: Реальное содержимое расписания (ТЗ п. 34: Preview на реальных данных)

    /// Дисциплины из ТЗ п. 15 — самые длинные в реальном расписании.
    static let longSubject = "Организационное и правовое обеспечение информационной безопасности"
    static let longerSubject = "Информационно-аналитические технологии поиска угроз информационной безопасности"
    static let modelingSubject = "Моделирование информационно-аналитических систем"

    // MARK: Даты

    /// Понедельник, 21 сентября 2026, 10:40 — опорная дата фикстур.
    static func moment(_ text: String) -> Date {
        let f = DateFormatter()
        f.timeZone = ScheduleClock.timeZone
        f.locale = Locale(identifier: "ru_RU")
        f.dateFormat = "yyyy-MM-dd HH:mm"
        return f.date(from: text) ?? Date()
    }

    private static var engine: ScheduleSelectionEngine {
        ScheduleSelectionEngine()
    }

    // MARK: Данные

    static func lesson(
        id: String,
        date: String,
        start: String,
        end: String,
        bell: Int,
        subject: String,
        type: String,
        room: String,
        teacher: String = "Иванов И. И."
    ) -> WLesson {
        WLesson(
            id: id, date: date, bell: bell, start: start, end: end,
            subject: subject, type: type, room: room, teacher: teacher
        )
    }

    /// Пары на один день — «обычный» учебный день.
    static func busyDay(_ date: String) -> [WLesson] {
        [
            lesson(id: "\(date)-1", date: date, start: "09:00", end: "10:30", bell: 1,
                   subject: "Базы данных", type: "ЛК", room: "341 (С-20)"),
            lesson(id: "\(date)-2", date: date, start: "10:40", end: "12:10", bell: 2,
                   subject: modelingSubject, type: "ПР", room: "341 (С-20)"),
            lesson(id: "\(date)-3", date: date, start: "12:40", end: "14:10", bell: 3,
                   subject: longSubject, type: "ЛК", room: "258а (С-20)"),
            lesson(id: "\(date)-4", date: date, start: "14:20", end: "15:50", bell: 4,
                   subject: longerSubject, type: "ЛАБ", room: "412 (В-78)")
        ]
    }

    static func target(_ lessons: [WLesson]) -> WTarget {
        WTarget(id: 1, title: "БИСО-02-23", typeName: "Группа", lessons: lessons)
    }

    // MARK: Сценарии

    private static func state(_ lessons: [WLesson], at text: String, target: WTarget? = nil) -> WidgetPresentationState {
        let t = target ?? self.target(lessons)
        let now = moment(text)
        return engine.makeState(target: t, now: now, generatedAt: now)
    }

    /// Пара идёт сейчас (10:40–12:10, смотрим в 11:00).
    static let current = WidgetPreviewFixture(
        name: "CURRENT",
        targetId: 1,
        state: state(busyDay("2026-09-21"), at: "2026-09-21 11:00")
    )

    /// Ближайшая пара впереди (смотрим в 09:30).
    static let upcoming = WidgetPreviewFixture(
        name: "UPCOMING",
        targetId: 1,
        state: state(busyDay("2026-09-21"), at: "2026-09-21 09:30")
    )

    /// До пары меньше порога — статус «ЧЕРЕЗ 25 МИН» (смотрим в 10:15).
    static let soon = WidgetPreviewFixture(
        name: "SOON",
        targetId: 1,
        state: state(busyDay("2026-09-21"), at: "2026-09-21 10:15")
    )

    /// Сегодня пар больше нет, показывается ближайший учебный день.
    static let futureDay = WidgetPreviewFixture(
        name: "FUTURE_DAY",
        targetId: 1,
        state: state(busyDay("2026-09-21") + busyDay("2026-09-23"), at: "2026-09-21 18:00")
    )

    /// Занятий нет в известном расписании.
    static let noSchedule = WidgetPreviewFixture(
        name: "NO_SCHEDULE",
        targetId: 1,
        state: state([], at: "2026-09-21 11:00")
    )

    /// Группа не выбрана (виджет только что добавлен).
    static let noTarget = WidgetPreviewFixture(
        name: "NO_TARGET",
        targetId: nil,
        state: engine.makeState(
            target: nil,
            now: moment("2026-09-21 11:00"),
            generatedAt: moment("2026-09-21 11:00")
        )
    )

    /// Самое длинное название — проверка переполнения (ТЗ п. 31).
    static let longTitle = WidgetPreviewFixture(
        name: "LONG_TITLE",
        targetId: 1,
        state: state(
            [
                lesson(id: "long-1", date: "2026-09-21", start: "10:40", end: "12:10", bell: 2,
                       subject: longerSubject, type: "ЛК", room: "258а (С-20)")
            ],
            at: "2026-09-21 11:00"
        )
    )

    /// Одна пара за весь день — остаток дня пуст.
    static let onlyOneLesson = WidgetPreviewFixture(
        name: "ONLY_ONE_LESSON",
        targetId: 1,
        state: state(
            [
                lesson(id: "one-1", date: "2026-09-21", start: "10:40", end: "12:10", bell: 2,
                       subject: "Базы данных", type: "ПР", room: "341 (С-20)")
            ],
            at: "2026-09-21 11:00"
        )
    )

    /// Много пар: проверка, что список не переполняет виджет (ТЗ п. 31).
    static let manyLessons = WidgetPreviewFixture(
        name: "MANY_LESSONS",
        targetId: 1,
        state: state(busyDay("2026-09-21") + busyDay("2026-09-22"), at: "2026-09-21 11:00")
    )

    /// Аудитория отсутствует — проверка подписи вместо пустоты (ТЗ п. 7).
    static let noRoom = WidgetPreviewFixture(
        name: "NO_ROOM",
        targetId: 1,
        state: state(
            [
                lesson(id: "noroom-1", date: "2026-09-21", start: "10:40", end: "12:10", bell: 2,
                       subject: "Базы данных", type: "ПР", room: "")
            ],
            at: "2026-09-21 11:00"
        )
    )

    /// Данные устарели — пометка о свежести (ТЗ п. 27).
    static let stale = WidgetPreviewFixture(
        name: "STALE",
        targetId: 1,
        state: {
            let now = moment("2026-09-21 11:00")
            return engine.makeState(
                target: target(busyDay("2026-09-21")),
                now: now,
                generatedAt: moment("2026-09-19 11:00")
            )
        }()
    )

    /// Все сценарии — для перебора в Preview и тестах.
    static let all: [WidgetPreviewFixture] = [
        current, upcoming, soon, futureDay, noSchedule, noTarget,
        longTitle, onlyOneLesson, manyLessons, noRoom, stale
    ]

    /// Данные для placeholder: система рисует их, пока нет настоящего снапшота.
    static var samplePayload: WPayload {
        WPayload(
            generatedAt: Int64(moment("2026-09-21 11:00").timeIntervalSince1970 * 1000),
            defaultTargetId: 1,
            targets: [target(busyDay("2026-09-21"))]
        )
    }
}