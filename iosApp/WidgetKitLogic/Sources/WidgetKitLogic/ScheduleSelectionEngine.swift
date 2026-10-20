import Foundation

// MARK: - РЎРѕСЃС‚РѕСЏРЅРёРµ РїСЂРµР·РµРЅС‚Р°С†РёРё

/// РџСЂРёС‡РёРЅР° РїСѓСЃС‚РѕРіРѕ СЃРѕСЃС‚РѕСЏРЅРёСЏ. РџРѕ РўР— (Рї. 27) В«РЅРµС‚ Р·Р°РЅСЏС‚РёР№В» Рё В«РЅРµС‚ РґР°РЅРЅС‹С…В» вЂ”
/// СЂР°Р·РЅС‹Рµ РІРµС‰Рё, Рё РїРѕРєР°Р·С‹РІР°С‚СЊ РёС… РѕРґРёРЅР°РєРѕРІРѕ Р·Р°РїСЂРµС‰РµРЅРѕ.
enum EmptyReason: Equatable {
    /// Р—Р°РЅСЏС‚РёР№ РЅРµС‚ РІ Р·Р°РіСЂСѓР¶РµРЅРЅРѕРј СЂР°СЃРїРёСЃР°РЅРёРё.
    case noLessons
    /// Р“СЂСѓРїРїР° РЅРµ РІС‹Р±СЂР°РЅР° РІРѕРІСЃРµ (РЅРµС‚ РєРѕРЅС„РёРіСѓСЂР°С†РёРё РІРёРґР¶РµС‚Р° Рё РЅРµС‚ РґРµС„РѕР»С‚Р°).
    case noTarget
    /// Р”Р°РЅРЅС‹С… РІ РєРѕРЅС‚РµР№РЅРµСЂРµ РµС‰С‘ РЅРµС‚ вЂ” РїСЂРёР»РѕР¶РµРЅРёРµ РЅРµ РїРёСЃР°Р»Рѕ СЃРЅР°РїС€РѕС‚.
    case noData
    /// Р”Р°РЅРЅС‹Рµ РµСЃС‚СЊ, РЅРѕ РЅРµ С‡РёС‚Р°СЋС‚СЃСЏ.
    case badData
    /// РќРµС‚ App Group РІ РїРѕРґРїРёСЃРё вЂ” РїРµСЂРµРїРѕРґРїРёСЃСЊ РЅРµ РґРѕР±Р°РІРёР»Р° РєРѕРЅС‚РµР№РЅРµСЂ.
    case noAppGroup
}

/// РЎРІРµР¶РµСЃС‚СЊ РґР°РЅРЅС‹С…: РїРѕ РўР— (Рї. 7) СѓСЃС‚Р°СЂРµРІС€РёР№ РєСЌС€ РјРѕР¶РЅРѕ РїРѕРєР°Р·С‹РІР°С‚СЊ, РЅРѕ
/// СЃ С‡РµСЃС‚РЅРѕР№ РїРѕРјРµС‚РєРѕР№, Р° РЅРµ РєР°Рє Р°РєС‚СѓР°Р»СЊРЅС‹Р№.
enum DataFreshness: Equatable {
    case fresh
    /// Р’РѕР·СЂР°СЃС‚ СЃРЅР°РїС€РѕС‚Р° РІ С‡Р°СЃР°С….
    case stale(hours: Int)
}

/// Р“РѕС‚РѕРІРѕРµ Рє РѕС‚РѕР±СЂР°Р¶РµРЅРёСЋ СЃРѕСЃС‚РѕСЏРЅРёРµ. Р’СЃРµ С‡РµС‚С‹СЂРµ РІРёРґР¶РµС‚Р° РїРѕР»СѓС‡Р°СЋС‚ РµРіРѕ РёР· РѕРґРЅРѕРіРѕ
/// РґРІРёР¶РєР° вЂ” СЌС‚Рѕ С‚СЂРµР±РѕРІР°РЅРёРµ РўР— (Рї. 19): РІС‹Р±РѕСЂРѕРј РїР°СЂС‹ РЅРµ РґРѕР»Р¶РЅР° Р·Р°РЅРёРјР°С‚СЊСЃСЏ РІС‘СЂСЃС‚РєР°.
struct WidgetPresentationState: Equatable {
    let generatedAt: Date
    let freshness: DataFreshness
    /// РќР°Р·РІР°РЅРёРµ РІС‹Р±СЂР°РЅРЅРѕРіРѕ СЂР°СЃРїРёСЃР°РЅРёСЏ (РґР»СЏ Р·Р°РіРѕР»РѕРІРєРѕРІ Рё РїСѓСЃС‚С‹С… СЃРѕСЃС‚РѕСЏРЅРёР№).
    let targetTitle: String?
    let primaryLesson: WLesson?
    let primaryStatus: LessonStatus?
    /// РџРѕРґРїРёСЃСЊ РґРЅСЏ РіР»Р°РІРЅРѕР№ РїР°СЂС‹. nil вЂ” РїР°СЂР° СЃРµРіРѕРґРЅСЏ.
    let primaryDayContext: DayContext
    /// РџРѕРґРїРёСЃСЊ С€Р°РїРєРё: В«РЎР•Р“РћР”РќРЇ В· РЎР Р•Р”РђВ», В«Р—РђР’РўР Рђ В· Р§Р•РўР’Р•Р Р“В», В«РџРў, 23 РћРљРўВ».
    let primaryDayLabel: String?
    /// РўР° Р¶Рµ РїРѕРґРїРёСЃСЊ, РЅРѕ РІСЃРµРіРґР° СЃ С‚РѕС‡РЅРѕР№ РґР°С‚РѕР№ вЂ” РґР»СЏ С€Р°РїРѕРє Medium Рё РєСЂСѓРїРЅРµРµ,
    /// РіРґРµ РјРµСЃС‚Рѕ РµСЃС‚СЊ Рё РјРµС‚РєР° В«РџРћРЎР›Р•Р—РђР’РўР РђВ» Р±РµР· РґР°С‚С‹ Р±С‹Р»Р° Р±С‹ РЅРµРїРѕР»РЅРѕР№ (РўР— Рї. 6).
    let primaryDayLongLabel: String?
    /// РћСЃС‚Р°Р»СЊРЅС‹Рµ Р·Р°РЅСЏС‚РёСЏ С‚РѕРіРѕ Р¶Рµ СѓС‡РµР±РЅРѕРіРѕ РґРЅСЏ, РІ РїРѕСЂСЏРґРєРµ СЃР»РµРґРѕРІР°РЅРёСЏ.
    let upcomingLessons: [WLesson]
    /// Р‘Р»РёР¶Р°Р№С€РёР№ СЃР»РµРґСѓСЋС‰РёР№ СѓС‡РµР±РЅС‹Р№ РґРµРЅСЊ (РЅРµ СЃРµРіРѕРґРЅСЏ).
    let nextStudyDay: DayContext
    /// Р—Р°РЅСЏС‚РёСЏ Р±Р»РёР¶Р°Р№С€РµРіРѕ СЃР»РµРґСѓСЋС‰РµРіРѕ СѓС‡РµР±РЅРѕРіРѕ РґРЅСЏ.
    let nextStudyDayLessons: [WLesson]
    /// Р•СЃС‚СЊ Р»Рё РµС‰С‘ Р·Р°РЅСЏС‚РёСЏ РїРѕРјРёРјРѕ РїРѕРєР°Р·Р°РЅРЅС‹С… (РґР»СЏ РїРѕРґРїРёСЃРё В«Р•С‰С‘ N РїР°СЂВ»).
    let hasMoreLessons: Bool
    /// Р—Р°РїРѕР»РЅРµРЅРѕ, С‚РѕР»СЊРєРѕ РµСЃР»Рё РїРѕРєР°Р·С‹РІР°С‚СЊ РЅРµС‡РµРіРѕ.
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

// MARK: - Р”РІРёР¶РѕРє

/// Р’С‹Р±РѕСЂ Р°РєС‚СѓР°Р»СЊРЅРѕР№ РїР°СЂС‹ Рё РїРѕРґРіРѕС‚РѕРІРєР° СЃРѕСЃС‚РѕСЏРЅРёСЏ РґР»СЏ РІСЊСЋ.
///
/// Р§РёСЃС‚Р°СЏ Р»РѕРіРёРєР°: РІСЂРµРјСЏ РїРµСЂРµРґР°С‘С‚СЃСЏ РїР°СЂР°РјРµС‚СЂРѕРј, Р·Р°РІРёСЃРёРјРѕСЃС‚Рё вЂ” С‚РѕР»СЊРєРѕ РєР°Р»РµРЅРґР°СЂСЊ
/// Рё Р·РѕРЅР° СЂР°СЃРїРёСЃР°РЅРёСЏ. Р­С‚Рѕ РїРѕР·РІРѕР»СЏРµС‚ РїСЂРѕРіРЅР°С‚СЊ РІСЃРµ СЃС†РµРЅР°СЂРёРё РўР— (Рї. 29) РЅР°
/// Р·Р°СЂР°РЅРµРµ РїРѕРґРіРѕС‚РѕРІР»РµРЅРЅС‹С… РґР°С‚Р°С… Р±РµР· РѕРїРѕСЂС‹ РЅР° СЃРёСЃС‚РµРјРЅРѕРµ РІСЂРµРјСЏ.
struct ScheduleSelectionEngine {
    /// РџРѕСЂРѕРі СЃС‚Р°С‚СѓСЃР° В«СЃРєРѕСЂРѕВ» (РўР— Рї. 4): 30 РјРёРЅСѓС‚ РґРѕ РЅР°С‡Р°Р»Р°.
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

    /// Р“Р»Р°РІРЅР°СЏ С‚РѕС‡РєР° РІС…РѕРґР°.
    ///
    /// - Parameters:
    ///   - target: РІС‹Р±СЂР°РЅРЅРѕРµ СЂР°СЃРїРёСЃР°РЅРёРµ (nil вЂ” РіСЂСѓРїРїР° РЅРµ РІС‹Р±СЂР°РЅР°).
    ///   - now: С‚РµРєСѓС‰РёР№ РјРѕРјРµРЅС‚.
    ///   - generatedAt: РєРѕРіРґР° СЃРЅР°РїС€РѕС‚ Р±С‹Р» Р·Р°РїРёСЃР°РЅ РїСЂРёР»РѕР¶РµРЅРёРµРј.
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

        // 1. РџР°СЂР°, РёРґСѓС‰Р°СЏ СЃРµР№С‡Р°СЃ.
        let current = lessons.first { isOngoing($0, at: now) }

        // 2. Р‘Р»РёР¶Р°Р№С€Р°СЏ РїСЂРµРґСЃС‚РѕСЏС‰Р°СЏ (РІ С‚РѕРј С‡РёСЃР»Рµ РІ Р±СѓРґСѓС‰РёРµ РґРЅРё).
        let upcoming = lessons.first { lesson in
            guard let start = lesson.startDate(timeZone: timeZone) else { return false }
            return start > now
        }

        let primary = current ?? upcoming
        guard let primary else {
            // Р’СЃРµ РїР°СЂС‹ СѓР¶Рµ РїСЂРѕС€Р»Рё: РїРѕРєР°Р·С‹РІР°РµРј СЃРїРѕРєРѕР№РЅРѕРµ РїСѓСЃС‚РѕРµ СЃРѕСЃС‚РѕСЏРЅРёРµ,
            // РЅРѕ СЃ РґР°С‚РѕР№ вЂ” С‡С‚Рѕ РёРјРµРЅРЅРѕ Р·Р°РєРѕРЅС‡РёР»РѕСЃСЊ (РўР— Рї. 27).
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

        // 3. РћСЃС‚Р°Р»СЊРЅС‹Рµ РїР°СЂС‹ С‚РѕРіРѕ Р¶Рµ СѓС‡РµР±РЅРѕРіРѕ РґРЅСЏ вЂ” РІ РїРѕСЂСЏРґРєРµ СЃР»РµРґРѕРІР°РЅРёСЏ.
        let sameDay = lessons.filter { calendar.isDate(dayStart($0), inSameDayAs: primaryDay) }
        let upcomingSameDay = sameDay.filter { lesson in
            guard let start = lesson.startDate(timeZone: timeZone) else { return false }
            return start > now && lesson.id != primary.id
        }

        // 4. Р‘Р»РёР¶Р°Р№С€РёР№ СЃР»РµРґСѓСЋС‰РёР№ СѓС‡РµР±РЅС‹Р№ РґРµРЅСЊ вЂ” РїРѕ РўР— (Рї. 6) СЌС‚Рѕ РЅРµ РѕР±СЏР·Р°С‚РµР»СЊРЅРѕ
        // СЃР»РµРґСѓСЋС‰РёР№ РєР°Р»РµРЅРґР°СЂРЅС‹Р№ РґРµРЅСЊ, Р° СЃР»РµРґСѓСЋС‰РёР№ РґРµРЅСЊ, РіРґРµ РµСЃС‚СЊ Р·Р°РЅСЏС‚РёСЏ.
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
            // РљРѕСЂРѕС‚РєР°СЏ РјРµС‚РєР° РґР»СЏ С‚РµСЃРЅС‹С… СЂР°Р·РјРµСЂРѕРІ (В«Р—РђР’РўР РђВ»), РїРѕР»РЅР°СЏ вЂ” РґР»СЏ
            // С€Р°РїРѕРє, РіРґРµ РµСЃС‚СЊ РјРµСЃС‚Рѕ (В«Р—РђР’РўР Рђ В· Р§Р•РўР’Р•Р Р“В»). РЎРµРіРѕРґРЅСЏС€РЅРёР№ РґРµРЅСЊ
            // РІСЃРµРіРґР° РїРѕРґРїРёСЃС‹РІР°РµС‚СЃСЏ РїРѕР»РЅС‹Рј Р·Р°РіРѕР»РѕРІРєРѕРј СЃ РґРЅС‘Рј РЅРµРґРµР»Рё.
            primaryDayLabel: isToday
                ? headerTodayLabel(primaryDay)
                : dayContext.label,
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

    // MARK: РџСЂРёРјРёС‚РёРІС‹

    /// РџР°СЂР° РёРґС‘С‚ СЃРµР№С‡Р°СЃ: `start <= now < end` (РўР— Рї. 4).
    ///
    /// РћСЃРѕР±С‹Р№ СЃР»СѓС‡Р°Р№ вЂ” РїРµСЂРµРјРµРЅС‹: Р·Р°РІРµСЂС€РёРІС€Р°СЏСЃСЏ РїР°СЂР° РіР»Р°РІРЅРѕР№ РЅРµ СЃС‡РёС‚Р°РµС‚СЃСЏ
    /// (РўР— Рї. 7), РїРѕСЌС‚РѕРјСѓ РІРµСЂС…РЅСЏСЏ РіСЂР°РЅРёС†Р° СЃС‚СЂРѕРіР°СЏ.
    func isOngoing(_ lesson: WLesson, at now: Date) -> Bool {
        guard let s = lesson.startDate(timeZone: timeZone),
              let e = lesson.endDate(timeZone: timeZone) else { return false }
        return now >= s && now < e
    }

    /// РџРѕСЂСЏРґРѕРє: РїРѕ РґР°С‚Рµ, Р·Р°С‚РµРј РїРѕ РІСЂРµРјРµРЅРё РЅР°С‡Р°Р»Р°, Р·Р°С‚РµРј РїРѕ РЅРѕРјРµСЂСѓ РїР°СЂС‹.
    /// `id` РІ РєРѕРЅС†Рµ вЂ” С‡С‚РѕР±С‹ РїРѕСЂСЏРґРѕРє Р±С‹Р» РґРµС‚РµСЂРјРёРЅРёСЂРѕРІР°РЅРЅС‹Рј РїСЂРё РїРѕР»РЅРѕРј СЃРѕРІРїР°РґРµРЅРёРё.
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

    /// РљРѕРЅС‚РµРєСЃС‚ РґРЅСЏ РѕС‚РЅРѕСЃРёС‚РµР»СЊРЅРѕ `now`.
    ///
    /// Р‘Р»РёР¶Р°Р№С€РёРµ РґРЅРё РїРѕР»СѓС‡Р°СЋС‚ Рё СЃР»РѕРІРµСЃРЅСѓСЋ РјРµС‚РєСѓ, Рё С‚РѕС‡РЅСѓСЋ РґР°С‚Сѓ: РўР— С‚СЂРµР±СѓРµС‚
    /// В«РџРћРЎР›Р•Р—РђР’РўР РђВ» (Рї. 16), РЅРѕ РІ РїСЂРёРјРµСЂРµ Рї. 6 РґР»СЏ РґРЅСЏ С‡РµСЂРµР· РґРІР° РґРЅСЏ Р¶РґС‘С‚
    /// В«РџРў, 23 РћРљРўВ». РџСЂРµРґСЃС‚Р°РІР»РµРЅРёРµ РІС‹Р±РёСЂР°РµС‚ РїРѕРґС…РѕРґСЏС‰СѓСЋ РїРѕРґРїРёСЃСЊ РїРѕ СЃРІРѕРµРјСѓ
    /// СЂР°Р·РјРµСЂСѓ, Р° РЅРµ С‚РµСЂСЏРµС‚ РёРЅС„РѕСЂРјР°С†РёСЋ.
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

    /// В«Р§Р•РўР’Р•Р Р“В» вЂ” РїРѕР»РЅРѕРµ РЅР°Р·РІР°РЅРёРµ РґРЅСЏ РЅРµРґРµР»Рё РґР»СЏ РїРѕРґРїРёСЃРё СЃ РґР°С‚РѕР№.
    func weekdayLabel(_ day: Date) -> String {
        let f = DateFormatter()
        f.calendar = calendar
        f.timeZone = timeZone
        f.locale = Locale(identifier: "ru_RU")
        f.dateFormat = "EEEE"
        return f.string(from: day).uppercased()
    }

    /// В«РџРў, 23 РћРљРўВ» вЂ” Р·Р°РіРѕР»РѕРІРѕРє РґР»СЏ РґРЅСЏ, РєРѕС‚РѕСЂС‹Р№ РЅРµ СЃРµРіРѕРґРЅСЏ Рё РЅРµ Р·Р°РІС‚СЂР°.
    ///
    /// Р СѓСЃСЃРєР°СЏ Р»РѕРєР°Р»СЊ СЃРѕРєСЂР°С‰Р°РµС‚ РјРµСЃСЏС†С‹ СЃ С‚РѕС‡РєРѕР№ (В«РѕРєС‚.В»), РЅРѕ РІ Р·Р°РіРѕР»РѕРІРєРµ
    /// РІРёРґР¶РµС‚Р° С„РѕСЂРјР° РёР· РўР— вЂ” Р±РµР· С‚РѕС‡РєРё, РїРѕСЌС‚РѕРјСѓ РµС‘ СЃСЂРµР·Р°РµРј.
    func longDateLabel(_ day: Date) -> String {
        let f = DateFormatter()
        f.calendar = calendar
        f.timeZone = timeZone
        f.locale = Locale(identifier: "ru_RU")
        f.dateFormat = "EE, d MMM"
        var text = f.string(from: day).uppercased()
        if text.hasSuffix(".") { text.removeLast() }
        return text
    }

    /// В«РЎР•Р“РћР”РќРЇ В· РЎР Р•Р”РђВ» вЂ” Р·Р°РіРѕР»РѕРІРѕРє С€Р°РїРєРё РґР»СЏ СЃРµРіРѕРґРЅСЏС€РЅРµРіРѕ РґРЅСЏ.
    private func headerTodayLabel(_ day: Date) -> String {
        let f = DateFormatter()
        f.calendar = calendar
        f.timeZone = timeZone
        f.locale = Locale(identifier: "ru_RU")
        f.dateFormat = "EEEE"
        return "РЎР•Р“РћР”РќРЇ В· " + f.string(from: day).uppercased()
    }
}