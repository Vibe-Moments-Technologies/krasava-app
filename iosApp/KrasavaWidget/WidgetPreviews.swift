import SwiftUI
import WidgetKit

// MARK: - Preview
//
// ТЗ п. 30: каждое представление проверяется на наборе состояний —
// CURRENT, UPCOMING, SOON, FUTURE_DAY, NO_SCHEDULE, NO_TARGET, LONG_TITLE,
// ONLY_ONE_LESSON, MANY_LESSONS, NO_ROOM, STALE.
//
// Preview использует ТОТ ЖЕ движок и те же вью, что и настоящий виджет,
// поэтому показывает фактическую компоновку, а не нарисованный макет.
// В Xcode: открыть этот файл и включить Canvas (⌥⌘↩).

/// Запись шкалы из фикстуры: дата берётся из сценария, чтобы подписи
/// «СЕЙЧАС»/«ЗАВТРА» соответствовали выбранному моменту.
private func entry(_ fixture: WidgetPreviewFixture) -> ScheduleEntry {
    ScheduleEntry(
        date: fixture.state.generatedAt,
        state: fixture.state,
        emptyReason: fixture.state.emptyReason,
        targetId: fixture.targetId
    )
}

/// Превью для одного размера на всех сценариях сразу.
private struct FixtureGallery: View {
    let family: WidgetFamily

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            ForEach(WidgetPreviewFixtures.all, id: \.name) { fixture in
                VStack(alignment: .leading, spacing: 4) {
                    Text(fixture.name)
                        .font(.caption2.bold())
                        .foregroundColor(.secondary)
                    ScheduleWidgetEntryView(entry: entry(fixture))
                        .environment(\.widgetFamily, family)
                        .frame(width: width, height: height)
                        .clipShape(RoundedRectangle(cornerRadius: 22))
                }
            }
        }
        .padding()
    }

    /// Приблизительные размеры семейств на iPhone — для читаемости превью.
    private var width: CGFloat {
        switch family {
        case .systemSmall: return 170
        case .systemMedium: return 364
        default: return 364
        }
    }

    private var height: CGFloat {
        switch family {
        case .systemSmall: return 170
        case .systemMedium: return 170
        default: return 382
        }
    }
}

// MARK: - Светлая и тёмная темы (ТЗ п. 30, 14)

#Preview("Small — все сценарии") {
    FixtureGallery(family: .systemSmall)
}

#Preview("Medium — все сценарии") {
    FixtureGallery(family: .systemMedium)
}

#Preview("Large — все сценарии") {
    FixtureGallery(family: .systemLarge)
}

#Preview("Small — тёмная тема") {
    FixtureGallery(family: .systemSmall)
        .preferredColorScheme(.dark)
}

#Preview("Large — тёмная тема") {
    FixtureGallery(family: .systemLarge)
        .preferredColorScheme(.dark)
}

// MARK: - Отдельные сценарии крупно

#Preview("CURRENT — Large") {
    ScheduleWidgetEntryView(entry: entry(WidgetPreviewFixtures.current))
        .environment(\.widgetFamily, .systemLarge)
        .frame(width: 364, height: 382)
        .clipShape(RoundedRectangle(cornerRadius: 22))
        .padding()
}

#Preview("LONG_TITLE — Small") {
    ScheduleWidgetEntryView(entry: entry(WidgetPreviewFixtures.longTitle))
        .environment(\.widgetFamily, .systemSmall)
        .frame(width: 170, height: 170)
        .clipShape(RoundedRectangle(cornerRadius: 22))
        .padding()
}

#Preview("NO_TARGET — Medium") {
    ScheduleWidgetEntryView(entry: entry(WidgetPreviewFixtures.noTarget))
        .environment(\.widgetFamily, .systemMedium)
        .frame(width: 364, height: 170)
        .clipShape(RoundedRectangle(cornerRadius: 22))
        .padding()
}

// MARK: - Виджет целиком (так система и покажет его в галерее)

#Preview("Виджет: Small", as: .systemSmall) {
    ScheduleWidget()
} timeline: {
    entry(WidgetPreviewFixtures.current)
    entry(WidgetPreviewFixtures.longTitle)
}

#Preview("Виджет: Medium", as: .systemMedium) {
    ScheduleWidget()
} timeline: {
    entry(WidgetPreviewFixtures.current)
    entry(WidgetPreviewFixtures.futureDay)
}

#Preview("Виджет: Large", as: .systemLarge) {
    ScheduleWidget()
} timeline: {
    entry(WidgetPreviewFixtures.current)
    entry(WidgetPreviewFixtures.manyLessons)
}