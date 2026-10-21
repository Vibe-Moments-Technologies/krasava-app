import SwiftUI
import WidgetKit
import AppIntents

// MARK: - Таймлайн
//
// По ТЗ (п. 21) запись содержит состояние, рассчитанное для её момента: система
// показывает готовый снимок, не пересчитывая выбор пары. Переходы планируются
// на значимых границах — начало и конец пары, полночь, — поэтому виджет
// обновляется сам без таймеров и без перезагрузки каждую минуту.

struct ScheduleEntry: TimelineEntry {
    let date: Date
    /// Готовое состояние на момент `date`. nil, если данных нет.
    let state: WidgetPresentationState?
    /// Причина отсутствия данных (когда `state` == nil).
    let emptyReason: EmptyReason?
    let targetId: Int?
}

// MARK: - Построение таймлайна

/// Собирает записи на несколько дней вперёд (дополнение владельца: расписание
/// предсказуемо, поэтому шкалу можно рассчитать заранее, а не просыпаться
/// каждую минуту). Система может показать запись позже указанного времени —
/// это допустимо, поэтому состояние считается для момента записи, а не «сейчас».
enum WidgetTimelineBuilder {
    /// Сколько записей формируем за раз. Больше не нужно: виджет живёт
    /// на домашнем экране, а при изменении расписания приложение само
    /// попросит перезагрузку.
    static let maxEntries = 40

    static func entries(
        targetId: Int?,
        now: Date,
        calendar: Calendar = Calendar(identifier: .gregorian)
    ) -> [ScheduleEntry] {
        // Моменты, когда меняется смысл содержимого.
        var dates: [Date] = [now]

        switch WidgetData.load() {
        case .ok(let payload):
            let target = WidgetData.resolveTarget(in: payload, selectedId: targetId)
                ?? payload.targets.first
            if let target {
                for lesson in target.lessons {
                    if let start = lesson.startDate(), start > now { dates.append(start) }
                    if let end = lesson.endDate(), end > now { dates.append(end) }
                }
            }
            _ = payload
        default:
            break
        }

        // Полночь: меняется подпись дня («СЕГОДНЯ» → «ЗАВТРА» → дата) и,
        // возможно, главная пара. Планируем на трое суток вперёд.
        for offset in 1...3 {
            if let midnight = calendar.date(
                byAdding: .day, value: offset,
                to: calendar.startOfDay(for: now)
            ) {
                dates.append(midnight)
            }
        }

        let unique = Array(Set(dates)).sorted().prefix(maxEntries)

        return unique.map { moment in
            let (context, reason) = WidgetData.context(selectedId: targetId, now: moment)
            return ScheduleEntry(
                date: moment,
                state: context?.state,
                emptyReason: reason,
                targetId: targetId
            )
        }
    }
}

// MARK: - Провайдер (iOS 17+, с выбором группы)

@available(iOS 17.0, *)
struct ScheduleWidgetProvider: AppIntentTimelineProvider {
    typealias Entry = ScheduleEntry
    typealias Intent = KrasavaWidgetIntent

    func placeholder(in context: Context) -> ScheduleEntry {
        ScheduleEntry(
            date: Date(),
            state: WidgetPreviewFixtures.current.state,
            emptyReason: nil,
            targetId: WidgetPreviewFixtures.current.targetId
        )
    }

    func snapshot(for configuration: KrasavaWidgetIntent, in context: Context) async -> ScheduleEntry {
        let targetId = resolved(configuration)
        let now = Date()
        let (ctx, reason) = WidgetData.context(selectedId: targetId, now: now)
        return ScheduleEntry(
            date: now, state: ctx?.state, emptyReason: reason, targetId: targetId
        )
    }

    func timeline(for configuration: KrasavaWidgetIntent, in context: Context) async -> Timeline<ScheduleEntry> {
        let targetId = resolved(configuration)
        let entries = WidgetTimelineBuilder.entries(targetId: targetId, now: Date())
        // .atEnd: система вернётся за новой шкалой после последней записи.
        return Timeline(entries: entries, policy: .atEnd)
    }

    /// Явный выбор в настройках виджета. nil означает «не выбран»: виджет
    /// берёт группу уведомлений (решение владельца — работает по умолчанию,
    /// и при этом выбор остаётся возможным через «Изменить виджет»).
    private func resolved(_ configuration: KrasavaWidgetIntent) -> Int? {
        configuration.target?.id
    }
}

// MARK: - Провайдер для iOS 15–16 (без конфигурации)

/// Совместимость: на этих версиях интерактивной настройки виджета нет вовсе,
/// поэтому расписание берётся из группы уведомлений.
struct LegacyScheduleWidgetProvider: TimelineProvider {
    func placeholder(in context: Context) -> ScheduleEntry {
        ScheduleEntry(
            date: Date(),
            state: WidgetPreviewFixtures.current.state,
            emptyReason: nil,
            targetId: nil
        )
    }

    func getSnapshot(in context: Context, completion: @escaping (ScheduleEntry) -> Void) {
        let now = Date()
        let (ctx, reason) = WidgetData.context(selectedId: nil, now: now)
        completion(ScheduleEntry(
            date: now, state: ctx?.state, emptyReason: reason, targetId: nil
        ))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<ScheduleEntry>) -> Void) {
        let entries = WidgetTimelineBuilder.entries(targetId: nil, now: Date())
        completion(Timeline(entries: entries, policy: .atEnd))
    }
}

// MARK: - Виджет

/// Один виджет в галерее. Выбор конфигурации — в обычной функции, а не в
/// result builder: `@WidgetBundleBuilder` не поддерживает ветвление по версии
/// ОС, а показывать интерактивный и legacy-виджет одновременно нельзя.
struct ScheduleWidget: Widget {
    let kind = "KrasavaWidget"

    var body: some WidgetConfiguration {
        configuration()
    }

    private func configuration() -> some WidgetConfiguration {
        if #available(iOS 17.0, *) {
            return AppIntentConfiguration(
                kind: kind,
                intent: KrasavaWidgetIntent.self,
                provider: ScheduleWidgetProvider()
            ) { entry in
                ScheduleWidgetEntryView(entry: entry)
            }
            .configurationDisplayName("Красава!")
            .description("Ближайшие пары: аудитория, время и дисциплина")
            .supportedFamilies(Self.families)
        } else {
            return StaticConfiguration(
                kind: kind,
                provider: LegacyScheduleWidgetProvider()
            ) { entry in
                ScheduleWidgetEntryView(entry: entry)
            }
            .configurationDisplayName("Красава!")
            .description("Ближайшие пары: аудитория, время и дисциплина")
            .supportedFamilies(Self.families)
        }
    }

    /// Размеры виджета.
    ///
    /// Четвёртый размер из ТЗ — `systemExtraLargePortrait` — в SDK iOS 26
    /// объявлен как `@available(iOS, unavailable)`: он существует только на
    /// visionOS, на iPhone его нет. Вместо него регистрируется
    /// `systemExtraLarge` — это iPad-размер (доступен с iOS 15), и по ТЗ
    /// п. 11.6 для iPad предусмотрена отдельная горизонтальная компоновка.
    /// На iPhone система просто не покажет его в галерее.
    static var families: [WidgetFamily] {
        [.systemSmall, .systemMedium, .systemLarge, .systemExtraLarge]
    }
}

// MARK: - Точка входа вью

/// Значение режима рендеринга, приведённое к типу без привязки к версии iOS.
///
/// `widgetRenderingMode` появился в iOS 16, а минимальная версия проекта —
/// 15.3: читаем его под проверкой доступности, иначе считаем полным цветом.
@available(iOS 16.0, *)
struct RenderingModeReader: View {
    let content: (WidgetPaletteMode) -> AnyView

    @Environment(\.widgetRenderingMode) private var renderingMode

    var body: some View {
        content(mode)
    }

    private var mode: WidgetPaletteMode {
        // В SDK iOS 26 у WidgetRenderingMode больше вариантов, чем fullColor /
        // accented / vibrant, и перечислять их по именам нельзя: набор растёт
        // от версии к версии. Всё, что не полный цвет, считаем тонированием —
        // это безопасный вариант: акценты станут монохромными, читаемость
        // обеспечивают текстовые метки статуса.
        if case .fullColor = renderingMode { return .fullColor }
        return .tinted
    }
}

/// Выбор представления по семейству. Вью получают уже готовое состояние
/// (ТЗ п. 19) — никакой логики выбора пары здесь нет.
struct ScheduleWidgetEntryView: View {
    @Environment(\.widgetFamily) var family
    let entry: ScheduleEntry

    var body: some View {
        Group {
            if #available(iOS 16.0, *) {
                // На iOS 16+ система может тонировать виджет: узнаём режим
                // через отдельный тип, объявленный с проверкой доступности.
                RenderingModeReader { mode in
                    AnyView(content(palette: WidgetPalette(mode: mode)))
                }
            } else {
                content(palette: WidgetPalette())
            }
        }
        .widgetContainerBackground()
    }

    @ViewBuilder
    private func content(palette: WidgetPalette) -> some View {
        if let state = entry.state {
            view(for: state, palette: palette)
        } else {
            // Данных нет вовсе: показываем пустое состояние с причиной,
            // а не пустой прямоугольник (ТЗ п. 27).
            WidgetEmptyStateView(
                reason: entry.emptyReason,
                targetTitle: nil,
                theme: WidgetTheme(),
                palette: palette
            )
        }
    }

    @ViewBuilder
    private func view(for state: WidgetPresentationState, palette: WidgetPalette) -> some View {
        switch family {
        case .systemMedium:
            MediumScheduleWidgetView(state: state, palette: palette)
        case .systemLarge:
            LargeScheduleWidgetView(state: state, palette: palette)
        case .systemExtraLarge:
            // iPad: горизонтальная панель (ТЗ п. 11.6). На iPhone этого
            // семейства нет, поэтому ветка срабатывает только на планшете.
            ExtraLargeScheduleWidgetView(state: state, palette: palette)
        default:
            SmallScheduleWidgetView(state: state, palette: palette)
        }
    }
}

// MARK: - Точка входа расширения

@main
struct KrasavaWidgetBundle: WidgetBundle {
    var body: some Widget {
        ScheduleWidget()
    }
}

// MARK: - Фон и совместимость

extension View {
    /// `containerBackground` обязателен с iOS 17: без него система не даёт
    /// виджету фон, и в режимах тонирования содержимое теряет читаемость
    /// (ТЗ п. 14). На 15–16 фон системный по умолчанию.
    @ViewBuilder
    func widgetContainerBackground() -> some View {
        if #available(iOSApplicationExtension 17.0, *) {
            self.containerBackground(for: .widget) {
                Color(.systemBackground)
            }
        } else {
            self
        }
    }
}