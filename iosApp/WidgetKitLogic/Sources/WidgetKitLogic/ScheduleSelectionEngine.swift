import Foundation

// MARK: - Состояние презентации

/// Причина пустого состояния. По ТЗ (п. 27) «нет занятий» и «нет данных» —
/// разные вещи, и показывать их одинаково запрещено.
enum EmptyReason: Equatable {
    /// Занятий нет в загруженном расписании.
    case noLessons
    /// Группа не выбрана вовсе (нет конфигурации виджета и нет дефолта).
    case noTarget
    /// Данных в контейнере ещё нет — приложение не писало снапшот.
    case noData
    /// Данные есть, но не читаются.
    case badData
    /// Нет App Group в подписи — переподпись не добавила контейнер.
    case noAppGroup
}

/// Свежесть данных: по ТЗ (п. 7) устаревший кэш можно показывать, но
/// с честной пометкой, а не как актуальный.
enum DataFreshness: Equatable {
    case fresh
    /// Возраст снапшота в часах.
    case stale(hours: Int)
}

/// Готовое к отображению состояние. Все четыре виджета получают его из одного
/// движка — это требование ТЗ (п. 19): выбором пары не должна заниматься вёрстка.
struct WidgetPresentationState: Equatable {
    let generatedAt: Date
    let freshness: DataFreshness
    /// Название выбранного расписания (для заголовков и пустых состояний).
    let targetTitle: String?
    let primaryLesson: WLesson?
    let primaryStatus: LessonStatus?
    /// Подпись дня главной пары. nil — пара сегодня.
    let primaryDayContext: DayContext
    /// Подпись шапки: «СЕГОДНЯ · СРЕДА», «ЗАВТРА · ЧЕТВЕРГ», «ПТ, 23 ОКТ».
    let primaryDayLabel: String?
    /// Та же подпись, но всегда с точной датой — для шапок Medium и крупнее,
    /// где место есть и метка «ПОСЛЕЗАВТРА» без даты была бы неполной (ТЗ п. 6).
    let primaryDayLongLabel: String?
    /// Остальные занятия того же учебного дня, в порядке следования.
    let upcomingLessons: [WLesson]
    /// Ближайший следующий учебный день (не сегодня).
    let nextStudyDay: DayContext
    /// Занятия ближайшего следующего учебного дня.
    let nextStudyDayLessons: [WLesson]
    /// Есть ли ещё занятия помимо показанных (для подписи «Ещё N пар»).
    let hasMoreLessons: Bool
    /// Заполнено, только если показывать нечего.
    let emptyReason: EmptyReason?

    static func empty(reason: EmptyReason, generatedAt: Date = Date()) -> WidgetPresentationState {
        WidgetPresentationState(
            generatedAt: generatedAt,
            freshness: .fresh,
            targetTitle: nil,
            primaryLesson: nil,
            primaryStatus: nil,
            primaryDayContext: .none,
            primaryDayLabel: nil,
            primaryDayLongLabel: nil,
            upcomingLessons: [],
            nextStudyDay: .none,
            nextStudyDayLessons: [],
            hasMoreLessons: false,
            emptyReason: reason
        )
    }

    var isEmpty: Bool { primaryLesson == nil }
}

// MARK: - Движок

/// Выбор актуальной пары и подготовка состояния для вью.
///
/// Чистая логика: время передаётся параметром, зависимости — только календарь
/// и зона расписания. Это позволяет прогнать все сценарии ТЗ (п. 29) на
/// заранее подготовленных датах без опоры на системное время.
struct ScheduleSelectionEngine {
    /// Порог статуса «скоро» (ТЗ п. 4): 30 минут до начала.
    static let soonThresholdMinutes = 30

    let calendar: Calendar
    let timeZone: TimeZone

    init(
        timeZone: TimeZone = ScheduleClock.timeZone,
        calendar: Calendar? = nil
    ) {
        self.timeZone = timeZone
        self.calendar = calendar ?? ScheduleClock.calendar(timeZone: timeZone)
    }

    /// Главная точка входа.
    ///
    /// - Parameters:
    ///   - target: выбранное расписание (nil — группа не выбрана).
    ///   - now: текущий момент.
    ///   - generatedAt: когда снапшот был записан приложением.
    func makeState(
        target: WTarget?,
        now: Date,
        generatedAt: Date,
        staleAfterHours: Int = 36
    ) -> WidgetPresentationState {
        guard let target else {
            return .empty(reason: .noTarget, generatedAt: generatedAt)
        }

        let lessons = ordered(target.lessons)
        guard !lessons.isEmpty else {
            return WidgetPresentationState(
                generatedAt: generatedAt,
                freshness: freshness(generatedAt: generatedAt, now: now, staleAfterHours: staleAfterHours),
                targetTitle: target.title,
                primaryLesson: nil,
                primaryStatus: nil,
                primaryDayContext: .none,
                primaryDayLabel: nil,
                primaryDayLongLabel: nil,
                upcomingLessons: [],
                nextStudyDay: .none,
                nextStudyDayLessons: [],
                hasMoreLessons: false,
                emptyReason: .noLessons
            )
        }

        let fresh = freshness(generatedAt: generatedAt, now: now, staleAfterHours: staleAfterHours)

        // 1. Пара, идущая сейчас.
        let current = lessons.first { isOngoing($0, at: now) }

        // 2. Ближайшая предстоящая (в том числе в будущие дни).
        let upcoming = lessons.first { lesson in
            guard let start = lesson.startDate(timeZone: timeZone) else { return false }
            return start > now
        }

        let primary = current ?? upcoming
        guard let primary else {
            // Все пары уже прошли: показываем спокойное пустое состояние,
            // но с датой — что именно закончилось (ТЗ п. 27).
            return WidgetPresentationState(
                generatedAt: generatedAt,
                freshness: fresh,
                targetTitle: target.title,
                primaryLesson: nil,
                primaryStatus: nil,
                primaryDayContext: .none,
                primaryDayLabel: nil,
                primaryDayLongLabel: nil,
                upcomingLessons: [],
                nextStudyDay: .none,
                nextStudyDayLessons: [],
                hasMoreLessons: false,
                emptyReason: .noLessons
            )
        }

        let primaryDay = dayStart(primary)
        let isToday = calendar.isDate(primaryDay, inSameDayAs: now)

        let status: LessonStatus
        if current != nil {
            status = .current
        } else if let start = primary.startDate(timeZone: timeZone) {
            let minutes = Int(start.timeIntervalSince(now) / 60)
            status = (0..<Self.soonThresholdMinutes).contains(minutes)
                ? .soon(minutesUntil: minutes)
                : .upcoming
        } else {
            status = .upcoming
        }

        // 3. Остальные пары того же учебного дня — в порядке следования.
        let sameDay = lessons.filter { calendar.isDate(dayStart($0), inSameDayAs: primaryDay) }
        let upcomingSameDay = sameDay.filter { lesson in
            guard let start = lesson.startDate(timeZone: timeZone) else { return false }
            return start > now && lesson.id != primary.id
        }

        // 4. Ближайший следующий учебный день — по ТЗ (п. 6) это не обязательно
        // следующий календарный день, а следующий день, где есть занятия.
        let futureDays = lessons
            .map { dayStart($0) }
            .filter { $0 > primaryDay }
        let nextDay = futureDays.min()
        let nextDayLessons = nextDay.map { day in
            lessons.filter { calendar.isDate(dayStart($0), inSameDayAs: day) }
        } ?? []

        let dayContext = context(for: primaryDay, now: now)

        return WidgetPresentationState(
            generatedAt: generatedAt,
            freshness: fresh,
            targetTitle: target.title,
            primaryLesson: primary,
            primaryStatus: status,
            primaryDayContext: dayContext,
            primaryDayLabel: isToday
                ? headerTodayLabel(primaryDay)
                : dayContext.label,
            // Полная подпись с точной датой — для шапок, где место есть.
            // Сегодняшний день всегда подписывается заголовком с днём недели.
            primaryDayLongLabel: isToday
                ? headerTodayLabel(primaryDay)
                : (dayContext.longLabel ?? dayContext.label),
            upcomingLessons: upcomingSameDay,
            nextStudyDay: nextDay.map { context(for: $0, now: now) } ?? .none,
            nextStudyDayLessons: nextDayLessons,
            hasMoreLessons: false,
            emptyReason: nil
        )
    }

    // MARK: Примитивы

    /// Пара идёт сейчас: `start <= now < end` (ТЗ п. 4).
    ///
    /// Особый случай — перемены: завершившаяся пара главной не считается
    /// (ТЗ п. 7), поэтому верхняя граница строгая.
    func isOngoing(_ lesson: WLesson, at now: Date) -> Bool {
        guard let s = lesson.startDate(timeZone: timeZone),
              let e = lesson.endDate(timeZone: timeZone) else { return false }
        return now >= s && now < e
    }

    /// Порядок: по дате, затем по времени начала, затем по номеру пары.
    /// `id` в конце — чтобы порядок был детерминированным при полном совпадении.
    func ordered(_ lessons: [WLesson]) -> [WLesson] {
        lessons.sorted { a, b in
            if a.date != b.date { return a.date < b.date }
            if a.start != b.start { return a.start < b.start }
            if a.bell != b.bell { return a.bell < b.bell }
            return a.id < b.id
        }
    }

    func dayStart(_ lesson: WLesson) -> Date {
        ScheduleClock.parseDay(lesson.date, calendar: calendar)
            ?? Date.distantPast
    }

    private func freshness(generatedAt: Date, now: Date, staleAfterHours: Int) -> DataFreshness {
        let hours = Int(now.timeIntervalSince(generatedAt) / 3600)
        return hours >= staleAfterHours ? .stale(hours: hours) : .fresh
    }

    /// Контекст дня относительно `now`.
    ///
    /// Ближайшие дни несут и словесную метку, и точную дату: ТЗ требует
    /// «ПОСЛЕЗАВТРА» (п. 16), но в примере п. 6 для того же дня ожидает
    /// «ПТ, 23 ОКТ». Представление выбирает подходящую подпись по своему
    /// размеру, поэтому требование «дата всегда понятна» выполняется всюду.
    func context(for day: Date, now: Date) -> DayContext {
        let today = calendar.startOfDay(for: now)
        let diff = calendar.dateComponents([.day], from: today, to: day).day ?? 0
        switch diff {
        case 0:
            return .today
        case 1:
            return .tomorrow(date: longDateLabel(day), weekday: weekdayLabel(day))
        case 2:
            return .dayAfterTomorrow(date: longDateLabel(day), weekday: weekdayLabel(day))
        case let d where d > 2:
            return .date(longDateLabel(day))
        default:
            return .none
        }
    }

    /// «ЧЕТВЕРГ» — полное название дня недели для подписи с датой.
    func weekdayLabel(_ day: Date) -> String {
        let f = DateFormatter()
        f.calendar = calendar
        f.timeZone = timeZone
        f.locale = Locale(identifier: "ru_RU")
        f.dateFormat = "EEEE"
        return f.string(from: day).uppercased()
    }

    /// «ПТ, 23 ОКТ» — заголовок для дня, который не сегодня и не завтра.
    ///
    /// Формат собирается вручную, а не через `MMM`: русская локаль даёт
    /// «нояб», «сент» и номер дня без ведущего нуля («2 НОЯБ»), тогда как
    /// интерфейс приложения использует «НОЯ», «СЕН» и двухзначный день.
    func longDateLabel(_ day: Date) -> String {
        let comps = calendar.dateComponents([.day, .month, .weekday], from: day)
        let weekday = Self.shortWeekdays[max(0, min(6, (comps.weekday ?? 1) - 1))]
        let dayNumber = String(format: "%02d", comps.day ?? 1)
        let month = Self.shortMonths[max(0, min(11, (comps.month ?? 1) - 1))]
        return "\(weekday), \(dayNumber) \(month)"
    }

    /// Сокращения дней недели (1 — воскресенье в григорианском календаре).
    private static let shortWeekdays = ["ВС", "ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ"]

    /// Сокращения месяцев в принятой в приложении форме.
    private static let shortMonths = [
        "ЯНВ", "ФЕВ", "МАР", "АПР", "МАЙ", "ИЮН",
        "ИЮЛ", "АВГ", "СЕН", "ОКТ", "НОЯ", "ДЕК"
    ]

    /// «СЕГОДНЯ · СРЕДА» — заголовок шапки для сегодняшнего дня.
    private func headerTodayLabel(_ day: Date) -> String {
        let f = DateFormatter()
        f.calendar = calendar
        f.timeZone = timeZone
        f.locale = Locale(identifier: "ru_RU")
        f.dateFormat = "EEEE"
        return "СЕГОДНЯ · " + f.string(from: day).uppercased()
    }
}