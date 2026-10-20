import Foundation

// MARK: - Модели виджета (зеркало WidgetPayload из shared)
//
// Файл намеренно зависит только от Foundation: те же исходники подключены
// к SwiftPM-пакету WidgetKitLogic, где на них гоняются unit-тесты. Всё, что
// тянет SwiftUI или WidgetKit, живёт в других файлах.

/// Пара расписания.
///
/// `id` приходит из shared (`Lesson.id` — стабильный идентификатор фида) и
/// нужен для deep links и для различения двух одинаковых пар подряд: по ТЗ
/// (п. 5) такие пары остаются разными записями, объединять их по названию
/// дисциплины запрещено.
struct WLesson: Decodable, Identifiable, Equatable {
    let id: String
    let date: String      // yyyy-MM-dd
    let bell: Int
    let start: String     // HH:mm
    let end: String
    let subject: String
    let type: String      // ЛК/ПР/ЛАБ/ДР/ДОП
    let room: String
    let teacher: String

    init(
        id: String,
        date: String,
        bell: Int,
        start: String,
        end: String,
        subject: String,
        type: String,
        room: String,
        teacher: String
    ) {
        self.id = id
        self.date = date
        self.bell = bell
        self.start = start
        self.end = end
        self.subject = subject
        self.type = type
        self.room = room
        self.teacher = teacher
    }

    /// Поле `id` в JSON может отсутствовать (старый снапшот в контейнере) —
    /// тогда синтезируем ключ из даты, времени и названия, чтобы виджет
    /// не падал на декодировании.
    enum CodingKeys: String, CodingKey {
        case id, date, bell, start, end, subject, type, room, teacher
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        date = try c.decode(String.self, forKey: .date)
        bell = try c.decode(Int.self, forKey: .bell)
        start = try c.decode(String.self, forKey: .start)
        end = try c.decode(String.self, forKey: .end)
        subject = try c.decode(String.self, forKey: .subject)
        type = try c.decode(String.self, forKey: .type)
        room = try c.decode(String.self, forKey: .room)
        teacher = try c.decode(String.self, forKey: .teacher)
        id = (try? c.decode(String.self, forKey: .id))
            ?? "\(date)-\(start)-\(subject)"
    }
}

/// Расписание, сохранённое в приложении (группа, преподаватель или аудитория).
struct WTarget: Decodable, Equatable {
    let id: Int
    let title: String
    let typeName: String  // «Группа» / «Преподаватель» / «Аудитория»
    let lessons: [WLesson]
}

/// Полный снапшот в общем контейнере.
struct WPayload: Decodable, Equatable {
    let generatedAt: Int64
    let defaultTargetId: Int?
    let targets: [WTarget]
}

// MARK: - Статус и контекст дня

/// Статус пары. Один источник правды о том, что показывать пользователю:
/// ни одно представление не должно решать это самостоятельно (ТЗ п. 19).
enum LessonStatus: Equatable {
    /// Пара идёт прямо сейчас.
    case current
    /// Ближайшая пара впереди.
    case upcoming
    /// До начала меньше порога — с обратным отсчётом.
    case soon(minutesUntil: Int)

    /// Метка на виджете. Обратный отсчёт допускается только там, где есть
    /// место (Medium и крупнее) — это решает представление, поэтому `soon`
    /// отдаёт оба варианта текста.
    var label: String {
        switch self {
        case .current: return "СЕЙЧАС"
        case .upcoming: return "ДАЛЕЕ"
        case .soon(let minutes):
            return minutes > 0 ? "ЧЕРЕЗ \(minutes) МИН" : "СКОРО"
        }
    }

    var isCurrent: Bool {
        if case .current = self { return true }
        return false
    }
}

/// Контекст даты для пары, которая не сегодня (ТЗ п. 6, 16).
///
/// Хранит и словесную метку, и точную дату: ТЗ требует «ПОСЛЕЗАВТРА» как
/// метку (п. 4, 16), но в примере п. 6 для дня через два дня ожидает подпись
/// «ПТ, 23 ОКТ». Конфликт разрешается по месту: короткая метка нужна там, где
/// тесно (Small), точная дата — где есть место (Medium и крупнее), а вместе
/// они всегда дают однозначность, требуемую п. 6 («дата всегда понятна»).
enum DayContext: Equatable {
    case today
    /// Завтра; строки — точная дата и день недели.
    case tomorrow(date: String, weekday: String)
    /// Послезавтра.
    case dayAfterTomorrow(date: String, weekday: String)
    /// Дальше: «ПН, 26 ОКТ».
    case date(String)
    case none

    /// Короткая метка для тесных размеров: «ЗАВТРА», «ПОСЛЕЗАВТРА».
    var label: String? {
        switch self {
        case .today, .none: return nil
        case .tomorrow: return "ЗАВТРА"
        case .dayAfterTomorrow: return "ПОСЛЕЗАВТРА"
        case .date(let text): return text
        }
    }

    /// Полная подпись с датой — для размеров, где есть место:
    /// «ЗАВТРА · ЧЕТВЕРГ», «ПОСЛЕЗАВТРА · ПТ, 23 ОКТ», «ПН, 26 ОКТ».
    var longLabel: String? {
        switch self {
        case .today, .none: return nil
        case .tomorrow(_, let weekday): return "ЗАВТРА · \(weekday)"
        case .dayAfterTomorrow(let date, _): return date
        case .date(let text): return text
        }
    }

    /// Точная дата, если она известна. Нужна для проверок и для подписи
    /// в заголовках, где словесная метка неуместна.
    var exactDate: String? {
        switch self {
        case .today, .none: return nil
        case .tomorrow(let date, _), .dayAfterTomorrow(let date, _): return date
        case .date(let text): return text
        }
    }

    /// День в будущем. Нужно для правила ТЗ п. 16: «ДАЛЕЕ» без даты для
    /// будущего дня недопустимо.
    var isFuture: Bool {
        switch self {
        case .today, .none: return false
        case .tomorrow, .dayAfterTomorrow, .date: return true
        }
    }
}

// MARK: - Время

/// Временная зона учебного расписания.
///
/// По ТЗ (п. 23) расчёты идут в зоне расписания, а не устройства: иначе
/// учебное `14:20` превращалось бы в другое время у студента в другой зоне.
enum ScheduleClock {
    /// Расписание РТУ МИРЭА живёт по московскому времени.
    static let timeZone = TimeZone(identifier: "Europe/Moscow") ?? .current

    /// Календарь расписания: зона зафиксирована, локаль — русская (интерфейс
    /// приложения русскоязычный, подписи дат должны совпадать с ним).
    static func calendar(timeZone: TimeZone = ScheduleClock.timeZone) -> Calendar {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        cal.locale = Locale(identifier: "ru_RU")
        return cal
    }

    /// Разбор «yyyy-MM-dd HH:mm» строго в зоне расписания.
    static func parse(date: String, time: String, timeZone: TimeZone = ScheduleClock.timeZone) -> Date? {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        let parts = time.split(separator: ":")
        guard parts.count == 2,
              let hour = Int(parts[0]),
              let minute = Int(parts[1]),
              let day = parseDay(date, calendar: cal) else { return nil }
        return cal.date(
            bySettingHour: hour, minute: minute, second: 0, of: day
        )
    }

    /// Начало суток для «yyyy-MM-dd».
    static func parseDay(_ text: String, calendar: Calendar) -> Date? {
        let parts = text.split(separator: "-")
        guard parts.count == 3,
              let year = Int(parts[0]),
              let month = Int(parts[1]),
              let day = Int(parts[2]) else { return nil }
        var comps = DateComponents()
        comps.year = year
        comps.month = month
        comps.day = day
        return calendar.date(from: comps)
    }
}

extension WLesson {
    /// Момент начала пары в зоне расписания.
    func startDate(timeZone: TimeZone = ScheduleClock.timeZone) -> Date? {
        ScheduleClock.parse(date: date, time: start, timeZone: timeZone)
    }

    /// Момент окончания пары. Если пара заканчивается после полуночи
    /// (переход через полночь), конец переносится на следующие сутки.
    func endDate(timeZone: TimeZone = ScheduleClock.timeZone) -> Date? {
        guard let s = startDate(timeZone: timeZone),
              let e = ScheduleClock.parse(date: date, time: end, timeZone: timeZone) else {
            return nil
        }
        if e < s {
            return Calendar.current.date(byAdding: .day, value: 1, to: e)
        }
        return e
    }

    /// Аудитория с корпусом, например «341 (С-20)». Пусто — аудитория не
    /// указана (СДО, дистант): по ТЗ (п. 7) вместо пустоты показываем
    /// понятную подпись, а не «— (—)».
    var roomLabel: String {
        room.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    /// Готовая подпись аудитории для интерфейса: либо номер, либо текст
    /// «Аудитория уточняется» / «Онлайн».
    var roomDisplay: String {
        let trimmed = roomLabel
        if trimmed.isEmpty { return "Аудитория уточняется" }
        // СДО — занятия в дистанционной среде; отдельного номера аудитории нет.
        let lower = trimmed.lowercased()
        if lower.contains("сдо") || lower.contains("дистант") || lower.contains("онлайн") {
            return "Онлайн"
        }
        return trimmed
    }

    /// Только номер аудитории без корпуса: «341 (С-20)» → «341».
    var roomNumber: String {
        let trimmed = roomLabel
        guard !trimmed.isEmpty else { return "" }
        if let paren = trimmed.firstIndex(of: "(") {
            return trimmed[trimmed.startIndex..<paren]
                .trimmingCharacters(in: .whitespaces)
        }
        return trimmed
    }

    /// Только корпус: «341 (С-20)» → «С-20».
    var buildingLabel: String {
        let trimmed = roomLabel
        guard let open = trimmed.firstIndex(of: "("),
              let close = trimmed.lastIndex(of: ")"),
              open < close else { return "" }
        return String(trimmed[trimmed.index(after: open)..<close])
            .trimmingCharacters(in: .whitespaces)
    }
}