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
//
// Семейство передаётся параметром, а не через environment: ключ
// `widgetFamily` доступен только для чтения, подменить его снаружи нельзя.

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
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                ForEach(WidgetPreviewFixtures.all, id: \.name) { fixture in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(fixture.name)
                            .font(.caption2.bold())
                            .foregroundColor(.secondary)
                        ScheduleWidgetEntryView(
                            entry: entry(fixture),
                            familyOverride: family
                        )
                        .frame(width: width, height: height)
                        .clipShape(RoundedRectangle(cornerRadius: 22))
                    }
                }
            }
            .padding()
        }
    }

    /// Приблизительные размеры семейств на iPhone — для читаемости превью.
    private var width: CGFloat {
        family == .systemSmall ? 170 : 364
    }

    private var height: CGFloat {
        switch family {
        case .systemSmall, .systemMedium: return 170
        default: return 382
        }
    }
}

// MARK: - Галереи по размерам

#Preview("Small — все сценарии") {
    FixtureGallery(family: .systemSmall)
}

#Preview("Medium — все сценарии") {
    FixtureGallery(family: .systemMedium)
}

#Preview("Large — все сценарии") {
    FixtureGallery(family: .systemLarge)
}

// MARK: - Темы (ТЗ п. 30, 14)

#Preview("Small — тёмная тема") {
    FixtureGallery(family: .systemSmall)
        .preferredColorScheme(.dark)
}

#Preview("Medium — тёмная тема") {
    FixtureGallery(family: .systemMedium)
        .preferredColorScheme(.dark)
}

#Preview("Large — тёмная тема") {
    FixtureGallery(family: .systemLarge)
        .preferredColorScheme(.dark)
}

// MARK: - Отдельные сценарии крупно

#Preview("CURRENT — Large") {
    ScheduleWidgetEntryView(entry: entry(WidgetPreviewFixtures.current), familyOverride: .systemLarge)
        .frame(width: 364, height: 382)
        .clipShape(RoundedRectangle(cornerRadius: 22))
        .padding()
}

#Preview("LONG_TITLE — Small") {
    ScheduleWidgetEntryView(entry: entry(WidgetPreviewFixtures.longTitle), familyOverride: .systemSmall)
        .frame(width: 170, height: 170)
        .clipShape(RoundedRectangle(cornerRadius: 22))
        .padding()
}

#Preview("NO_TARGET — Medium") {
    ScheduleWidgetEntryView(entry: entry(WidgetPreviewFixtures.noTarget), familyOverride: .systemMedium)
        .frame(width: 364, height: 170)
        .clipShape(RoundedRectangle(cornerRadius: 22))
        .padding()
}

#Preview("FUTURE_DAY — Small") {
    ScheduleWidgetEntryView(entry: entry(WidgetPreviewFixtures.futureDay), familyOverride: .systemSmall)
        .frame(width: 170, height: 170)
        .clipShape(RoundedRectangle(cornerRadius: 22))
        .padding()
}

#Preview("NO_ROOM — Small") {
    ScheduleWidgetEntryView(entry: entry(WidgetPreviewFixtures.noRoom), familyOverride: .systemSmall)
        .frame(width: 170, height: 170)
        .clipShape(RoundedRectangle(cornerRadius: 22))
        .padding()
}