import Foundation
import SwiftUI
import WidgetKit

// MARK: - Модель (зеркало WidgetPayload из shared)

struct WLesson: Decodable {
    let date: String      // yyyy-MM-dd
    let bell: Int
    let start: String     // HH:mm
    let end: String
    let subject: String
    let type: String      // ЛК/ПР/ЛАБ/ДР/ДОП
    let room: String
    let teacher: String
}

struct WTarget: Decodable {
    let id: Int
    let title: String
    let typeName: String  // «Группа» / «Преподаватель» / «Аудитория»
    let lessons: [WLesson]
}

struct WPayload: Decodable {
    let generatedAt: Int64
    let defaultTargetId: Int?
    let targets: [WTarget]
}

// MARK: - Чтение общего контейнера

enum WidgetData {
    /// Ключ снапшота. Старый "widget_snapshot" (одно активное расписание)
    /// не читаем: контракт изменился на список всех сохранённых расписаний.
    static let key = "widget_payload"

    enum State {
        /// У процесса нет entitlements на App Group — переподпись не добавила группу.
        case noGroup
        /// Группа есть, но приложение ещё ничего не написало.
        case noData
        /// Группа есть, данные есть, но не читаются.
        case badData
        case ok(WPayload)
    }

    static func load() -> State {
        // Группа определяется из профиля подписи (см. AppGroupLocator):
        // приложение может быть переподписано любым инструментом.
        guard let suite = AppGroupLocator.availableSuiteName() else { return .noGroup }
        guard let defaults = UserDefaults(suiteName: suite),
              let json = defaults.string(forKey: key) else { return .noData }
        guard let payload = try? JSONDecoder().decode(WPayload.self, from: Data(json.utf8)) else {
            return .badData
        }
        return .ok(payload)
    }

    /// Список расписаний для экрана «Изменить виджет»: пустой массив, если
    /// данных ещё нет (система покажет «Нет групп» вместо падения).
    static func availableTargets() -> [WTarget] {
        if case .ok(let payload) = load() { return payload.targets }
        return []
    }

    /// Расписание строго по выбранному id. nil — пользователь ещё не выбрал
    /// группу (или выбранная удалена в приложении). Намеренно БЕЗ фолбэка:
    /// виджет обязан показать подсказку «Выберите группу», а не угадывать.
    static func target(in payload: WPayload, id: Int?) -> WTarget? {
        guard let id else { return nil }
        return payload.targets.first { $0.id == id }
    }

    /// Расписание для старого (iOS 15–16) виджета, где конфигурации нет:
    /// группа уведомлений, иначе первое сохранённое — обратная совместимость.
    static func legacyTargetId(in payload: WPayload) -> Int? {
        payload.defaultTargetId ?? payload.targets.first?.id
    }
}

// MARK: - Время

private let dayFmt: DateFormatter = {
    let f = DateFormatter()
    f.dateFormat = "yyyy-MM-dd HH:mm"
    return f
}()

private let dayOnlyFmt: DateFormatter = {
    let f = DateFormatter()
    f.dateFormat = "yyyy-MM-dd"
    return f
}()

private let shortDateFmt: DateFormatter = {
    let f = DateFormatter()
    f.setLocalizedDateFormatFromTemplate("d MMM")
    return f
}()

extension WLesson {
    var startDate: Date? { dayFmt.date(from: "\(date) \(start)") }
    var endDate: Date? { dayFmt.date(from: "\(date) \(end)") }

    func isCurrent(at now: Date) -> Bool {
        guard let s = startDate, let e = endDate else { return false }
        return now >= s && now <= e
    }

    func isUpcoming(at now: Date) -> Bool {
        guard let s = startDate else { return false }
        return s > now
    }

    var isToday: Bool { date == dayOnlyFmt.string(from: Date()) }

    var isTomorrow: Bool {
        guard let tomorrow = Calendar.current.date(byAdding: .day, value: 1, to: Date()) else {
            return false
        }
        return date == dayOnlyFmt.string(from: tomorrow)
    }

    /// Метка дня для правого края служебной строки: сегодня — ничего,
    /// завтра — «завтра», дальше — «пт 24».
    var dayMarker: String? {
        if isToday { return nil }
        if isTomorrow { return "завтра" }
        guard let parsed = dayOnlyFmt.date(from: date) else { return nil }
        return shortDateFmt.string(from: parsed).lowercased()
    }

    var typeColor: Color {
        switch type {
        case "ЛК": return .blue
        case "ПР": return .green
        case "ЛАБ": return .orange
        case "ДОП": return .pink
        default: return .gray
        }
    }

    /// «375 (С-20)» или пусто — для карточек без аудитории (СДО, дистант).
    var roomLabel: String {
        let trimmed = room.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? "" : trimmed
    }
}