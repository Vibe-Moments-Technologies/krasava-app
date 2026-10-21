import Foundation

// MARK: - Состояние презентации

/// Причина пустого состояния. По ТЗ (п. 10.4) «нет занятий» и «нет данных» —
/// разные вещи, показывать их одинаково запрещено.
enum EmptyReason: Equatable {
    /// Занятий нет в загруженном расписании.
    case noLessons
    /// Группа не выбрана вовсе.
    case noTarget
    /// Данных в контейнере ещё нет — приложение не писало снапшот.
    case noData
    /// Данные есть, но не читаются.
    case badData
    /// Нет App Group в подписи — переподпись не добавила контейнер.
    case noAppGroup
}

/// Свежесть данных: устаревший кэш можно показывать, но с честной пометкой.
enum DataFreshness: Equatable {
    case fresh
    /// Возраст снапшота в часах.
    case stale(hours: Int)

    var isStale: Bool {
        if case .stale = self { return true }
        return false
    }
}

/// Учебный день в готовом виде: подпись и занятия.
struct WidgetDay: Equatable {
    /// Подпись для заголовка: «ПЯТНИЦА · 23 ОКТЯБРЯ».
    let label: String
    /// Занятия дня в хронологическом порядке.
    let lessons: [WLesson]
    /// Дата дня («yyyy-MM-dd») — для заголовков и будущих deep links.
    let date: String
}

/// Готовое к отображению состояние.
///
/// Все четыре виджета получают его из одного движка — это требование ТЗ
/// (п. 11): выбором активной пары не должна заниматься вёрстка.
struct WidgetPresentationState: Equatable {
    let generatedAt: Date
    let freshness: DataFreshness
    /// Название выбранного расписания (для пустых состояний).
    let targetTitle: String?

    /// Активная пара: текущая, иначе ближайшая предстоящая, иначе первая
    /// пара ближайшего учебного дня (ТЗ п. 1.1).
    let primaryLesson: WLesson?
    let primaryStatus: LessonStatus?
    /// День активной пары.
    let primaryDay: DayContext
    /// Заголовок выбранного учебного дня: «СЕГОДНЯ · СРЕДА».
    let primaryDayLabel: String?

    /// Остальные занятия того же учебного дня (активная исключена).
    let upcomingLessons: [WLesson]

    /// Ближайшие следующие учебные дни с занятиями, по порядку.
    /// Первый — ближайший; второй добавляется, только если он есть.
    let nextDays: [WidgetDay]

    /// Заполнено, только если показывать нечего.
    let emptyReason: EmptyReason?

    static func empty(reason: EmptyReason, generatedAt: Date = Date()) -> WidgetPresentationState {
        WidgetPresentationState(
            generatedAt: generatedAt,
            freshness: .fresh,
            targetTitle: nil,
            primaryLesson: nil,
            primaryStatus: nil,
            primaryDay: .none,
            primaryDayLabel: nil,
            upcomingLessons: [],
            nextDays: [],
            emptyReason: reason
        )
    }

    var isEmpty: Bool { primaryLesson == nil }

    /// Полный заголовок дня для шапок Large/Extra Large.
    var headerTitle: String {
        primaryDayLabel ?? primaryDay.fullLabel ?? primaryDay.shortLabel ?? "СЕГОДНЯ"
    }
}

// MARK: - Движок

/// Выбор активной пары и подготовка состояния для вью.
///
/// Чистая логика: время передаётся параметром, зависимости — только календарь
/// и зона расписания. Это позволяет прогнать все сценарии ТЗ (п. 18) на
/// заранее подготовленных датах без опоры на системные часы.
struct ScheduleSelectionEngine {
    /// Порог статуса «СКОРО» (ТЗ п. 12.3): 30 минут до начала, включительно.
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
    ///   - now: текущий момент (в тестах — заранее заданный).
    ///   - generatedAt: когда снапшот был записан приложением.
    ///   - staleAfterHours: с какого возраста данные считаются устаревшими.
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
        let fresh = freshness(generatedAt: generatedAt, now: now, staleAfterHours: staleAfterHours)

        guard !lessons.isEmpty else {
            return WidgetPresentationState(
                generatedAt: generatedAt,
                freshness: fresh,
                targetTitle: target.title,
                primaryLesson: nil,
                primaryStatus: nil,
                primaryDay: .none,
                primaryDayLabel: nil,
                upcomingLessons: [],
                nextDays: [],
                emptyReason: .noLessons
            )
        }

        // 1. Активная пара: текущая → ближайшая предстоящая.
        let current = lessons.first { isOngoing($0, at: now) }
        let upcoming = lessons.first { lesson in
            guard let start = lesson.startDate(timeZone: timeZone) else { return false }
            return start > now
        }
        let primary = current ?? upcoming

        guard let primary else {
            // Все пары уже прошли: показывать нечего.
            return WidgetPresentationState(
                generatedAt: generatedAt,
                freshness: fresh,
                targetTitle: target.title,
                primaryLesson: nil,
                primaryStatus: nil,
                primaryDay: .none,
                primaryDayLabel: nil,
                upcomingLessons: [],
                nextDays: [],
                emptyReason: .noLessons
            )
        }

        let primaryDayStart = dayStart(primary)
        let isToday = calendar.isDate(primaryDayStart, inSameDayAs: now)

        let status = status(for: primary, current: current, now: now)
        let dayContext = context(for: primaryDayStart, now: now)

        // 2. Остальные занятия того же учебного дня, кроме активной.
        let sameDayLessons = lessons.filter {
            calendar.isDate(dayStart($0), inSameDayAs: primaryDayStart)
        }
        let upcomingSameDay = sameDayLessons.filter { lesson in
            lesson.id != primary.id && (lesson.startDate(timeZone: timeZone) ?? .distantPast) > now
        }

        // 3. Следующие учебные дни. По ТЗ (п. 11.3) перебираем календарные
        //    даты через Calendar, а не прибавлением 86400 секунд.
        let futureDays = nextStudyDays(after: primaryDayStart, in: lessons, now: now, limit: 2)

        return WidgetPresentationState(
            generatedAt: generatedAt,
            freshness: fresh,
            targetTitle: target.title,
            primaryLesson: primary,
            primaryStatus: status,
            primaryDay: dayContext,
            primaryDayLabel: isToday ? todayHeader(primaryDayStart) : dayContext.fullLabel,
            upcomingLessons: upcomingSameDay,
            nextDays: futureDays,
            emptyReason: nil
        )
    }

    // MARK: Примитивы

    /// Пара идёт сейчас: `start <= now < end` (ТЗ п. 11.1).
    ///
    /// Верхняя граница строгая: в момент окончания пара уже завершена,
    /// поэтому во время перемены активной становится следующая.
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
        ScheduleClock.parseDay(lesson.date, calendar: calendar) ?? .distantPast
    }

    private func status(for primary: WLesson, current: WLesson?, now: Date) -> LessonStatus {
        if current != nil { return .current }
        guard let start = primary.startDate(timeZone: timeZone) else { return .upcoming }
        let minutes = Int(start.timeIntervalSince(now) / 60)
        // Порог включительный: «30 минут до начала» — это уже «СКОРО».
        return (0...Self.soonThresholdMinutes).contains(minutes)
            ? .soon(minutesUntil: minutes)
            : .upcoming
    }

    private func freshness(generatedAt: Date, now: Date, staleAfterHours: Int) -> DataFreshness {
        let hours = Int(now.timeIntervalSince(generatedAt) / 3600)
        return hours >= staleAfterHours ? .stale(hours: hours) : .fresh
    }

    /// Ближайшие учебные дни после указанного, в порядке следования.
    ///
    /// Пустые календарные дни пропускаются: виджет переходит к ближайшему
    /// дню, где занятия действительно есть (ТЗ п. 1.1, 11.3).
    func nextStudyDays(
        after day: Date,
        in lessons: [WLesson],
        now: Date,
        limit: Int
    ) -> [WidgetDay] {
        var dayStarts: [Date] = []
        for lesson in lessons {
            let start = dayStart(lesson)
            guard start > day else { continue }
            if !dayStarts.contains(where: { calendar.isDate($0, inSameDayAs: start) }) {
                dayStarts.append(start)
            }
        }

        return dayStarts.sorted().prefix(limit).map { startOfDay in
            let dayLessons = lessons.filter {
                calendar.isDate(self.dayStart($0), inSameDayAs: startOfDay)
            }
            return WidgetDay(
                label: weekdayDateLabel(startOfDay),
                lessons: dayLessons,
                date: dateString(startOfDay)
            )
        }
    }

    /// Контекст дня относительно `now`.
    func context(for day: Date, now: Date) -> DayContext {
        let today = calendar.startOfDay(for: now)
        let diff = calendar.dateComponents([.day], from: today, to: day).day ?? 0
        switch diff {
        case 0:
            return .today
        case 1:
            return .tomorrow(full: weekdayDateLabel(day))
        case 2:
            return .dayAfterTomorrow(full: weekdayDateLabel(day))
        default:
            return .date(full: weekdayDateLabel(day))
        }
    }

    // MARK: Форматирование дат (ТЗ п. 11.4)

    /// Полные названия месяцев в родительном падеже — как в макетах
    /// («23 ОКТЯБРЯ»). Русская локаль через DateFormatter даёт другие формы.
    private static let months = [
        "ЯНВАРЯ", "ФЕВРАЛЯ", "МАРТА", "АПРЕЛЯ", "МАЯ", "ИЮНЯ",
        "ИЮЛЯ", "АВГУСТА", "СЕНТЯБРЯ", "ОКТЯБРЯ", "НОЯБРЯ", "ДЕКАБРЯ"
    ]

    /// Полные названия дней недели.
    private static let weekdays = [
        "ВОСКРЕСЕНЬЕ", "ПОНЕДЕЛЬНИК", "ВТОРНИК", "СРЕДА",
        "ЧЕТВЕРГ", "ПЯТНИЦА", "СУББОТА"
    ]

    /// «ПЯТНИЦА · 23 ОКТЯБРЯ» — подпись учебного дня.
    ///
    /// Формат собирается вручную: DateFormatter с русской локалью даёт
    /// «пятница, 23 октября» — строчными и с запятой, а макет требует
    /// заглавных и разделителя «·».
    func weekdayDateLabel(_ day: Date) -> String {
        let comps = calendar.dateComponents([.day, .month, .weekday], from: day)
        let weekday = Self.weekdays[max(0, min(6, (comps.weekday ?? 1) - 1))]
        let month = Self.months[max(0, min(11, (comps.month ?? 1) - 1))]
        return "\(weekday) · \(comps.day ?? 1) \(month)"
    }

    /// «СЕГОДНЯ · СРЕДА» — заголовок для сегодняшнего дня.
    func todayHeader(_ day: Date) -> String {
        let comps = calendar.dateComponents([.weekday], from: day)
        let weekday = Self.weekdays[max(0, min(6, (comps.weekday ?? 1) - 1))]
        return "СЕГОДНЯ · \(weekday)"
    }

    /// Дата в формате снапшота «yyyy-MM-dd».
    func dateString(_ day: Date) -> String {
        let comps = calendar.dateComponents([.year, .month, .day], from: day)
        return String(
            format: "%04d-%02d-%02d",
            comps.year ?? 1970, comps.month ?? 1, comps.day ?? 1
        )
    }
}
