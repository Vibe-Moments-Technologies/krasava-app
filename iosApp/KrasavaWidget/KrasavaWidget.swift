import SwiftUI
import WidgetKit
import AppIntents

// MARK: - Таймлайн

struct Entry: TimelineEntry {
    let date: Date
    let payload: WPayload?
    /// Выбранное в настройках виджета расписание (nil — не выбрано / iOS 15–16).
    let targetId: Int?
    let state: WidgetData.State

    static func preview(_ payload: WPayload? = nil, targetId: Int? = nil, date: Date = Date()) -> Entry {
        Entry(date: date, payload: payload, targetId: targetId,
              state: payload.map { .ok($0) } ?? .noData)
    }
}

struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> Entry {
        Entry.preview(SampleData.payload, targetId: SampleData.targetId)
    }

    func getSnapshot(in context: Context, completion: @escaping (Entry) -> Void) {
        let state = WidgetData.load()
        let payload = payload(from: state)
        completion(Entry(
            date: Date(),
            payload: payload,
            targetId: payload.flatMap(WidgetData.legacyTargetId(in:)),
            state: state
        ))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<Entry>) -> Void) {
        let state = WidgetData.load()
        let payload = payload(from: state)
        // iOS 15–16: выбор группы недоступен, поэтому берём расписание
        // уведомлений (обратная совместимость) — подсказки не показываем.
        let targetId = payload.flatMap(WidgetData.legacyTargetId(in:))
        let now = Date()
        var dates: [Date] = []
        // Границы пар: виджет перерисуется ровно в момент смены пары.
        let lessons = payload.flatMap { WidgetData.target(in: $0, id: targetId)?.lessons } ?? []
        for lesson in lessons {
            if let start = lesson.startDate, start > now { dates.append(start) }
            if let end = lesson.endDate, end > now { dates.append(end) }
        }
        if let midnight = Calendar.current.date(
            byAdding: .day, value: 1,
            to: Calendar.current.startOfDay(for: now)
        ) {
            dates.append(midnight)
        }
        dates.append(now.addingTimeInterval(3600))

        var entries = [Entry(date: now, payload: payload, targetId: targetId, state: state)]
        entries += dates.sorted().prefix(10).map {
            Entry(date: $0, payload: payload, targetId: targetId, state: state)
        }
        completion(Timeline(entries: entries, policy: .atEnd))
    }

    private func payload(from state: WidgetData.State) -> WPayload? {
        if case .ok(let payload) = state { return payload }
        return nil
    }
}

// MARK: - Общие элементы

/// Служебная строка пары: время · тип · аудитория.
///
/// Аудитория прижата к правому краю и объявлена layout-приоритетом: раньше
/// весь ряд лежал в одном HStack, и метка дня выдавливала аудиторию за
/// край виджета — «347 (С-20)» превращалось в «34…». Метку дня теперь несёт
/// предмет (см. lessonBlock), а не эта строка.
private func metaLine(_ lesson: WLesson, at now: Date) -> some View {
    HStack(spacing: 5) {
        Text("\(lesson.start)–\(lesson.end)")
            .font(.system(size: 11, weight: .semibold))
            .monospacedDigit()
            .foregroundColor(lesson.isCurrent(at: now) ? .green : .secondary)
            .layoutPriority(1)

        Text(lesson.type)
            .font(.system(size: 10, weight: .bold))
            .foregroundColor(lesson.typeColor)
            .padding(.horizontal, 4)
            .padding(.vertical, 0.5)
            .background(lesson.typeColor.opacity(0.15))
            .clipShape(Capsule())
            .layoutPriority(1)

        Spacer(minLength: 4)

        if !lesson.roomLabel.isEmpty {
            // Аудитория обязательна: минимум усечения, максимум читаемости.
            Text(lesson.roomLabel)
                .font(.system(size: 11, weight: .bold))
                .foregroundColor(.primary)
                .lineLimit(1)
                .minimumScaleFactor(0.75)
                .layoutPriority(2)
        }
    }
}

/// Пара целиком: служебная строка + предмет во всю ширину.
/// Метка дня («завтра», «пт 24») ушла во вторую строку — там есть место,
/// и она больше не конкурирует с аудиторией за ширину.
private func lessonBlock(_ lesson: WLesson, at now: Date, showDay: Bool) -> some View {
    VStack(alignment: .leading, spacing: 1) {
        metaLine(lesson, at: now)
        HStack(alignment: .firstTextBaseline, spacing: 4) {
            Text(lesson.subject)
                .font(.system(size: 13, weight: .semibold))
                .lineLimit(2)
                .minimumScaleFactor(0.85)
                .fixedSize(horizontal: false, vertical: true)
                // Предмет важнее метки дня: при нехватке ширины ужимается
                // в первую очередь метка, а не название дисциплины.
                .layoutPriority(1)
            if showDay, let marker = lesson.dayMarker {
                Text(marker)
                    .font(.system(size: 10, weight: .medium))
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            }
        }
    }
}

private func typeBadge(_ lesson: WLesson, size: CGFloat = 11) -> some View {
    Text(lesson.type)
        .font(.system(size: size, weight: .bold))
        .foregroundColor(lesson.typeColor)
        .padding(.horizontal, 5)
        .padding(.vertical, 1)
        .background(lesson.typeColor.opacity(0.15))
        .clipShape(Capsule())
}

/// Подсказка вместо пустого виджета: заголовок, объяснение, иконка.
private func hintView(
    icon: String,
    title: String,
    text: String,
    tint: Color = .secondary
) -> some View {
    VStack(spacing: 6) {
        Image(systemName: icon)
            .font(.title2)
            .foregroundColor(tint)
        Text(title)
            .font(.system(size: 13, weight: .bold))
            .multilineTextAlignment(.center)
        Text(text)
            .font(.system(size: 11))
            .foregroundColor(.secondary)
            .multilineTextAlignment(.center)
    }
    .padding(4)
}

// MARK: - Виджет

struct KrasavaWidgetView: View {
    @Environment(\.widgetFamily) var family
    var entry: Entry

    var body: some View {
        Group {
            if let payload = entry.payload {
                let target = WidgetData.target(in: payload, id: entry.targetId)
                if let target, !target.lessons.isEmpty {
                    content(target)
                } else if entry.targetId == nil {
                    // iOS 17+ без выбора группы: осознанно пустой виджет
                    // с инструкцией, куда нажать. Legacy-провайдер сюда не
                    // попадает — он всегда подставляет группу уведомлений.
                    hintView(
                        icon: "slider.horizontal.3",
                        title: "Выберите группу",
                        text: "Долгое нажатие → «Изменить виджет» → Группа"
                    )
                } else {
                    VStack(spacing: 4) {
                        Image(systemName: "checkmark.circle")
                            .font(.title2)
                            .foregroundColor(.green)
                        Text("Пар впереди нет")
                            .font(.system(size: 13, weight: .bold))
                        Text(target?.title ?? "")
                            .font(.system(size: 11))
                            .foregroundColor(.secondary)
                            .lineLimit(1)
                    }
                    .padding(4)
                }
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
            hintView(
                icon: "exclamationmark.shield",
                title: "Нет App Group в подписи",
                text: "Переподпишите приложение профилем, который разрешает App Group из entitlements обоих бандлов",
                tint: .orange
            )
        case .badData:
            hintView(
                icon: "exclamationmark.triangle",
                title: "Данные не читаются",
                text: "Откройте приложение и сверните его",
                tint: .orange
            )
        case .noData, .ok:
            hintView(
                icon: "calendar.badge.plus",
                title: "Нет данных",
                text: "Добавьте расписание в приложении"
            )
        }
    }

    @ViewBuilder
    private func content(_ target: WTarget) -> some View {
        let upcoming = ordered(target.lessons, at: entry.date)
        switch family {
        case .systemSmall:
            smallView(target: target, lessons: upcoming)
        case .systemMedium:
            listView(target: target, lessons: Array(upcoming.prefix(3)))
        case .systemLarge:
            // 6 пар: замер по вертикали (345pt минус padding и шапка) —
            // больше не влезет и обрежется снизу.
            largeView(target: target, lessons: Array(upcoming.prefix(6)))
        default:
            smallView(target: target, lessons: upcoming)
        }
    }

    /// Порядок показа: идущая сейчас пара, затем ближайшие. Если сегодня пар
    /// больше нет — идём в следующие дни (поэтому фильтра по дате нет).
    private func ordered(_ lessons: [WLesson], at now: Date) -> [WLesson] {
        let current = lessons.filter { $0.isCurrent(at: now) }
        let future = lessons.filter { $0.isUpcoming(at: now) }
        return current + future
    }

    /// Small: одна пара — текущая или ближайшая. Крупно, предмет не обрезается.
    @ViewBuilder
    private func smallView(target: WTarget, lessons: [WLesson]) -> some View {
        if let lesson = lessons.first {
            VStack(alignment: .leading, spacing: 3) {
                HStack(spacing: 5) {
                    Text(lesson.isCurrent(at: entry.date) ? "СЕЙЧАС" : "ДАЛЕЕ")
                        .font(.system(size: 10, weight: .heavy))
                        .foregroundColor(lesson.isCurrent(at: entry.date) ? .green : .secondary)
                    Spacer(minLength: 0)
                    typeBadge(lesson, size: 10)
                }

                Text(lesson.subject)
                    .font(.system(size: 14, weight: .bold))
                    .lineLimit(3)
                    // Без fixedSize: он заставляет текст занять полную высоту
                    // и мешает minimumScaleFactor ужать строку вместо обрезки.
                    .minimumScaleFactor(0.7)
                    .layoutPriority(1)

                Spacer(minLength: 0)

                // Время и аудитория одной строкой: раньше это были две строки,
                // и они съедали высоту, из-за которой предмет обрезался.
                HStack(alignment: .firstTextBaseline, spacing: 5) {
                    Text("\(lesson.start)–\(lesson.end)")
                        .font(.system(size: 12, weight: .semibold))
                        .monospacedDigit()
                        .foregroundColor(lesson.isCurrent(at: entry.date) ? .green : .primary)
                    if !lesson.roomLabel.isEmpty {
                        Text(lesson.roomLabel)
                            .font(.system(size: 12, weight: .bold))
                            .lineLimit(1)
                            .minimumScaleFactor(0.75)
                    }
                    Spacer(minLength: 0)
                }

                HStack(spacing: 4) {
                    Text(target.title)
                        .font(.system(size: 10))
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                    if let marker = lesson.dayMarker {
                        Text("· \(marker)")
                            .font(.system(size: 10))
                            .foregroundColor(.secondary)
                    }
                }
            }
            .padding(10)
        }
    }

    /// Medium: до трёх пар двумя строками — служебная + предмет во всю ширину.
    /// Отступы и межстрочный интервал подобраны по высоте medium (~160pt):
    /// шапка 16 + три пары по ~40 + зазоры 6×2 = 148pt, плюс padding 10×2.
    @ViewBuilder
    private func listView(target: WTarget, lessons: [WLesson]) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            header(target, lessons: lessons)

            ForEach(Array(lessons.enumerated()), id: \.offset) { _, lesson in
                lessonBlock(lesson, at: entry.date, showDay: true)
            }
            Spacer(minLength: 0)
        }
        .padding(10)
    }

    /// Large: до шести пар с разделителями дней — «Сегодня» / «Завтра» / дата.
    @ViewBuilder
    private func largeView(target: WTarget, lessons: [WLesson]) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            header(target, lessons: lessons)

            ForEach(Array(lessons.enumerated()), id: \.offset) { index, lesson in
                if showsDayHeader(at: index, in: lessons) {
                    Text(dayHeader(for: lesson))
                        .font(.system(size: 10, weight: .heavy))
                        .foregroundColor(.secondary)
                        .padding(.top, index == 0 ? 0 : 4)
                }
                lessonBlock(lesson, at: entry.date, showDay: false)
            }
            Spacer(minLength: 0)
        }
        .padding(10)
    }

    @ViewBuilder
    private func header(_ target: WTarget, lessons: [WLesson]) -> some View {
        HStack(spacing: 4) {
            Text(target.title)
                .font(.system(size: 12, weight: .bold))
                .lineLimit(1)
            Text(target.typeName)
                .font(.system(size: 10))
                .foregroundColor(.secondary)
            Spacer(minLength: 0)
            if lessons.contains(where: { $0.isCurrent(at: entry.date) }) {
                Text("идёт пара")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(.green)
            }
        }
    }

    private func showsDayHeader(at index: Int, in lessons: [WLesson]) -> Bool {
        guard index < lessons.count else { return false }
        if index == 0 { return true }
        return lessons[index - 1].date != lessons[index].date
    }

    private func dayHeader(for lesson: WLesson) -> String {
        if lesson.isToday { return "СЕГОДНЯ" }
        if lesson.isTomorrow { return "ЗАВТРА" }
        return lesson.dayMarker?.uppercased() ?? lesson.date
    }
}

// MARK: - Совместимость

extension View {
    /// containerBackground обязателен с iOS 17; на 15–16 просто фон по умолчанию.
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

/// Единственный виджет в галерее. Выбор конфигурации — внутри `body`, потому
/// что `@WidgetBundleBuilder` не поддерживает ветвление: показывать оба
/// виджета сразу нельзя (в галерее были бы две «Красава!»).
///
/// iOS 17+ — AppIntentConfiguration: пользователь выбирает расписание в
/// «Изменить виджет». iOS 15–16 — статическая конфигурация с расписанием
/// уведомлений: интерактивная настройка там недоступна в принципе.
struct KrasavaWidget: Widget {
    let kind = "KrasavaWidget"

    var body: some WidgetConfiguration {
        // Тип возвращает одна функция, а не result builder: он не принимает
        // `if #available` напрямую, а оборачивать конфигурацию в @ViewBuilder
        // нельзя.
        configuration()
    }

    private func configuration() -> some WidgetConfiguration {
        if #available(iOS 17.0, *) {
            return AppIntentConfiguration(
                kind: kind,
                intent: KrasavaWidgetIntent.self,
                provider: IntentProvider()
            ) { entry in
                KrasavaWidgetView(entry: entry)
            }
            .configurationDisplayName("Красава!")
            .description("Пары выбранной группы, преподавателя или аудитории")
            .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
        } else {
            return StaticConfiguration(kind: kind, provider: Provider()) { entry in
                KrasavaWidgetView(entry: entry)
            }
            .configurationDisplayName("Красава!")
            .description("Пары расписания, выбранного для уведомлений")
            .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
        }
    }
}

@main
struct KrasavaWidgetBundle: WidgetBundle {
    var body: some Widget {
        KrasavaWidget()
    }
}

// MARK: - Провайдер интерактивного виджета

@available(iOS 17.0, *)
struct IntentProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> Entry {
        Entry.preview(SampleData.payload, targetId: SampleData.targetId)
    }

    func snapshot(for configuration: KrasavaWidgetIntent, in context: Context) async -> Entry {
        let state = WidgetData.load()
        return Entry(
            date: Date(),
            payload: payload(from: state),
            targetId: configuration.target?.id,
            state: state
        )
    }

    func timeline(for configuration: KrasavaWidgetIntent, in context: Context) async -> Timeline<Entry> {
        let state = WidgetData.load()
        let payload = payload(from: state)
        let targetId = configuration.target?.id
        let now = Date()

        var dates: [Date] = []
        let lessons = payload.flatMap { WidgetData.target(in: $0, id: targetId)?.lessons } ?? []
        for lesson in lessons {
            if let start = lesson.startDate, start > now { dates.append(start) }
            if let end = lesson.endDate, end > now { dates.append(end) }
        }
        if let midnight = Calendar.current.date(
            byAdding: .day, value: 1,
            to: Calendar.current.startOfDay(for: now)
        ) {
            dates.append(midnight)
        }
        dates.append(now.addingTimeInterval(3600))

        var entries = [Entry(date: now, payload: payload, targetId: targetId, state: state)]
        entries += dates.sorted().prefix(10).map {
            Entry(date: $0, payload: payload, targetId: targetId, state: state)
        }
        return Timeline(entries: entries, policy: .atEnd)
    }

    private func payload(from state: WidgetData.State) -> WPayload? {
        if case .ok(let payload) = state { return payload }
        return nil
    }
}

// MARK: - Данные для галереи виджетов

enum SampleData {
    static let targetId = 1

    static let payload = WPayload(
        generatedAt: 0,
        defaultTargetId: targetId,
        targets: [
            WTarget(
                id: targetId,
                title: "БИСО-02-23",
                typeName: "Группа",
                lessons: [
                    WLesson(date: todayString(), bell: 2, start: "10:40", end: "12:10",
                            subject: "Формализованные модели и методы проектирования",
                            type: "ЛК", room: "А-4 (В-78)", teacher: "Иванов И.И."),
                    WLesson(date: todayString(), bell: 3, start: "12:40", end: "14:10",
                            subject: "Базы данных", type: "ПР", room: "375 (С-20)",
                            teacher: "Петров П.П."),
                    WLesson(date: todayString(), bell: 4, start: "14:20", end: "15:50",
                            subject: "Теория вероятностей", type: "ЛАБ", room: "412 (В-78)",
                            teacher: ""),
                ]
            )
        ]
    )

    private static func todayString() -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        return f.string(from: Date())
    }
}