import WidgetKit
import SwiftUI

// MARK: - Модель (зеркало WidgetSnapshot из shared)

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

struct WSnapshot: Decodable {
    let generatedAt: Int64
    let targetTitle: String
    let lessons: [WLesson]
}

enum WidgetData {
    static let key = "widget_snapshot"

    enum State {
        /// У процесса нет entitlements на App Group — переподпись не добавила группу.
        case noGroup
        /// Группа есть, но приложение ещё ничего не написало (или у приложения нет группы).
        case noData
        /// Группа есть, данные есть, но не читаются.
        case badData
        case ok(WSnapshot)
    }

    static func load() -> State {
        // Группа определяется из профиля подписи (см. AppGroupLocator):
        // приложение может быть переподписано любым инструментом.
        guard let suite = AppGroupLocator.availableSuiteName() else { return .noGroup }
        guard let defaults = UserDefaults(suiteName: suite),
              let json = defaults.string(forKey: key) else { return .noData }
        guard let snap = try? JSONDecoder().decode(WSnapshot.self, from: Data(json.utf8)) else {
            return .badData
        }
        return .ok(snap)
    }
}

// MARK: - Время

private let dayFmt: DateFormatter = {
    let f = DateFormatter()
    f.dateFormat = "yyyy-MM-dd HH:mm"
    return f
}()

private let weekdayFmt: DateFormatter = {
    let f = DateFormatter()
    f.dateFormat = "EEEE"
    return f
}()

private let dayNumFmt: DateFormatter = {
    let f = DateFormatter()
    f.dateFormat = "d"
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

    var isTomorrow: Bool {
        date == dayFmt.string(from: Calendar.current.date(byAdding: .day, value: 1, to: Date()) ?? Date()).prefix(10).description
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
}

// MARK: - Timeline

struct Entry: TimelineEntry {
    let date: Date
    let snapshot: WSnapshot?
    let state: WidgetData.State

    /// Для снапшота галереи виджетов и превью — «идеальный» вариант.
    static func preview(_ snapshot: WSnapshot? = nil, date: Date = Date()) -> Entry {
        Entry(date: date, snapshot: snapshot, state: snapshot.map { .ok($0) } ?? .noData)
    }
}

struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> Entry {
        Entry.preview()
    }

    func getSnapshot(in context: Context, completion: @escaping (Entry) -> Void) {
        let state = WidgetData.load()
        let snap: WSnapshot?
        if case .ok(let s) = state { snap = s } else { snap = nil }
        completion(Entry(date: Date(), snapshot: snap, state: state))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<Entry>) -> Void) {
        let state = WidgetData.load()
        let snap: WSnapshot?
        if case .ok(let s) = state { snap = s } else { snap = nil }
        let now = Date()
        var dates: [Date] = []
        // Границы пар: начало/конец каждой предстоящей — виджет обновится
        // ровно в момент смены пары.
        for l in snap?.lessons ?? [] {
            if let s = l.startDate, s > now { dates.append(s) }
            if let e = l.endDate, e > now { dates.append(e) }
        }
        // Полночь + почасовая страховка (на случай рассинхрона данных).
        let midnight = Calendar.current.date(byAdding: .day, value: 1, to: Calendar.current.startOfDay(for: now))
        if let midnight = midnight {
            dates.append(midnight)
        }
        dates.append(now.addingTimeInterval(3600))

        var entries = [Entry(date: now, snapshot: snap, state: state)]
        entries += dates.sorted().prefix(10).map { Entry(date: $0, snapshot: snap, state: state) }
        completion(Timeline(entries: entries, policy: .atEnd))
    }
}

// MARK: - Вьюхи

/// Бейдж типа пары: ЛК/ПР/ЛАБ в цвете типа.
private func typeBadge(_ lesson: WLesson) -> some View {
    Text(lesson.type)
        .font(.caption2).bold()
        .foregroundColor(lesson.typeColor)
        .padding(.horizontal, 5).padding(.vertical, 1)
        .background(lesson.typeColor.opacity(0.15))
        .clipShape(Capsule())
}

/// Время пары одной строкой; зелёное, если пара идёт сейчас.
private func timeRange(_ lesson: WLesson, at now: Date) -> some View {
    Text("\(lesson.start)–\(lesson.end)")
        .font(.caption2).bold()
        .foregroundColor(lesson.isCurrent(at: now) ? .green : .primary)
}

struct LessonRow: View {
    let lesson: WLesson
    /// compact (medium): всё в одну строку — время, бейдж, предмет, аудитория.
    /// full (large): предмет в две строки, дата и аудитория второй строкой.
    let compact: Bool
    let now: Date

    var body: some View {
        HStack(spacing: 6) {
            timeRange(lesson, at: now)
                .frame(width: 72, alignment: .leading)
            typeBadge(lesson)
            if compact {
                Text(lesson.subject)
                    .font(.caption).bold()
                    .lineLimit(1)
                Spacer(minLength: 4)
                if !lesson.room.isEmpty {
                    Text(lesson.room)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }
            } else {
                VStack(alignment: .leading, spacing: 1) {
                    Text(lesson.subject)
                        .font(.caption).bold()
                        .lineLimit(2)
                    HStack(spacing: 4) {
                        Text(lesson.isTomorrow ? "завтра" : "сегодня")
                        if !lesson.room.isEmpty {
                            Text(lesson.room)
                        }
                    }
                    .font(.caption2)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
                }
                Spacer(minLength: 0)
            }
        }
    }
}

struct KrasavaWidgetView: View {
    @Environment(\.widgetFamily) var family
    var entry: Entry

    var body: some View {
        Group {
            if let snap = entry.snapshot {
                content(snap)
            } else {
                emptyState
            }
        }
        .containerBackgroundCompat()
    }

    @ViewBuilder
    private var emptyState: some View {
        switch entry.state {
        case .noGroup:
            VStack(spacing: 6) {
                Image(systemName: "exclamationmark.shield")
                    .font(.title2)
                    .foregroundColor(.orange)
                Text("Нет App Group в подписи")
                    .font(.caption).bold()
                Text("Переподпишите приложение профилем, который разрешает App Group из entitlements обоих бандлов")
                    .font(.caption2)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            .padding()
        case .badData:
            VStack(spacing: 6) {
                Image(systemName: "exclamationmark.triangle")
                    .font(.title2)
                    .foregroundColor(.orange)
                Text("Данные виджета не читаются")
                    .font(.caption).bold()
                Text("Обновите снапшот: откройте приложение и сверните его")
                    .font(.caption2)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            .padding()
        case .noData, .ok:
            VStack(spacing: 6) {
                Image(systemName: "calendar.badge.plus")
                    .font(.title2)
                    .foregroundColor(.secondary)
                Text("Добавьте расписание в приложении")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            .padding()
        }
    }

    @ViewBuilder
    private func content(_ snap: WSnapshot) -> some View {
        let now = entry.date
        let current = snap.lessons.first { $0.isCurrent(at: now) }
        let upcoming = snap.lessons.filter { $0.isUpcoming(at: now) }
        let all = (current.map { [$0] } ?? []) + upcoming

        switch family {
        case .systemSmall:
            smallView(snap: snap, current: current, upcoming: upcoming)
        case .systemMedium:
            listView(snap: snap, lessons: Array(all.prefix(3)), compact: true)
        case .systemLarge:
            listView(snap: snap, lessons: Array(all.prefix(10)), compact: false)
        default:
            smallView(snap: snap, current: current, upcoming: upcoming)
        }
    }

    @ViewBuilder
    private func smallView(snap: WSnapshot, current: WLesson?, upcoming: [WLesson]) -> some View {
        if let lesson = current ?? upcoming.first {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 4) {
                    timeRange(lesson, at: entry.date)
                    Spacer(minLength: 0)
                    typeBadge(lesson)
                }
                Text(lesson.subject)
                    .font(.caption).bold()
                    .lineLimit(3)
                    .minimumScaleFactor(0.8)
                Spacer(minLength: 0)
                if !lesson.room.isEmpty {
                    Text(lesson.room)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }
            }
            .padding(12)
        } else {
            VStack(spacing: 6) {
                Image(systemName: "checkmark.circle")
                    .font(.title2)
                    .foregroundColor(.green)
                Text("Пар больше нет")
                    .font(.caption).bold()
                Text(snap.targetTitle)
                    .font(.caption2)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            }
            .padding(12)
        }
    }

    @ViewBuilder
    private func listView(snap: WSnapshot, lessons: [WLesson], compact: Bool) -> some View {
        if compact {
            // Medium: колонка даты слева (как в «Календаре»), плотные строки справа.
            HStack(alignment: .top, spacing: 10) {
                VStack(alignment: .leading, spacing: 0) {
                    Text(weekdayFmt.string(from: entry.date))
                        .font(.caption2).bold()
                        .textCase(.uppercase)
                        .foregroundColor(.red)
                    Text(dayNumFmt.string(from: entry.date))
                        .font(.system(size: 30, weight: .heavy))
                    Spacer(minLength: 4)
                    Text(snap.targetTitle)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                }
                .frame(width: 74, alignment: .leading)

                VStack(alignment: .leading, spacing: 6) {
                    if lessons.isEmpty {
                        Spacer()
                        Text("Пар больше нет — отдыхаем 🎉")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Spacer()
                    } else {
                        ForEach(Array(lessons.enumerated()), id: \.offset) { _, lesson in
                            LessonRow(lesson: lesson, compact: true, now: entry.date)
                        }
                        Spacer(minLength: 0)
                    }
                }
            }
            .padding(12)
        } else {
            // Large: шапка с группой и список полных строк.
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text(snap.targetTitle)
                        .font(.caption).bold()
                        .lineLimit(1)
                    Spacer()
                    if let current = lessons.first, current.isCurrent(at: entry.date) {
                        Text("идёт пара")
                            .font(.caption2).bold()
                            .foregroundColor(.green)
                    }
                }
                if lessons.isEmpty {
                    Spacer()
                    Text("Пар больше нет — отдыхаем 🎉")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Spacer()
                } else {
                    ForEach(Array(lessons.enumerated()), id: \.offset) { _, lesson in
                        LessonRow(lesson: lesson, compact: false, now: entry.date)
                    }
                    Spacer(minLength: 0)
                }
            }
            .padding(12)
        }
    }
}

// containerBackground обязателен с iOS 17; на 15–16 просто фон по умолчанию.
extension View {
    @ViewBuilder
    func containerBackgroundCompat() -> some View {
        if #available(iOSApplicationExtension 17.0, *) {
            self.containerBackground(for: .widget) { Color(.systemBackground) }
        } else {
            self
        }
    }
}

// MARK: - Конфигурация

@main
struct KrasavaWidget: Widget {
    let kind = "KrasavaWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Provider()) { entry in
            KrasavaWidgetView(entry: entry)
        }
        .configurationDisplayName("Красава!")
        .description("Текущая и ближайшие пары")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}
