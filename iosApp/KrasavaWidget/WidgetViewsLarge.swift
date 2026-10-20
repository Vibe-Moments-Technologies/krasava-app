import SwiftUI

// MARK: - Large: день целиком
//
// Назначение (ТЗ п. 10): актуальная пара и расписание на оставшуюся часть
// учебного дня. Шапка подписывает день, главная пара выделена сильнее
// остальных, ниже — компактный список, а не карточки на каждую пару
// (ТЗ п. 10.2: уплотнить список без потери читаемости).

struct LargeScheduleWidgetView: View {
    let state: WidgetPresentationState
    let palette: WidgetPalette

    var body: some View {
        GeometryReader { geo in
            let theme = theme(for: geo.size.height)
            content(theme: theme, height: geo.size.height)
                .frame(width: geo.size.width, height: geo.size.height, alignment: .topLeading)
        }
    }

    @ViewBuilder
    private func content(theme: WidgetTheme, height: CGFloat) -> some View {
        VStack(alignment: .leading, spacing: theme.blockSpacing) {
            WidgetDayHeader(
                text: state.primaryDayLabel ?? "СЕГОДНЯ",
                theme: theme,
                palette: palette,
                trailing: state.freshness.isStale ? "данные устарели" : nil
            )

            if let lesson = state.primaryLesson, let status = state.primaryStatus {
                primary(lesson: lesson, status: status, theme: theme)

                if !state.upcomingLessons.isEmpty {
                    WidgetDivider(palette: palette)
                    WidgetSectionHeader(
                        text: sectionTitle,
                        theme: theme,
                        palette: palette
                    )
                    // Количество пар определяется доступной высотой, а не
                    // константой (ТЗ п. 10.3): считаем по фактическому размеру,
                    // чтобы не выводить обрезанную пару снизу.
                    let rows = rowCount(height: height, theme: theme)
                    ForEach(Array(state.upcomingLessons.prefix(rows)), id: \.id) { item in
                        CompactLessonRow(lesson: item, theme: theme, palette: palette)
                    }
                    if state.upcomingLessons.count > rows {
                        Text("Ещё \(state.upcomingLessons.count - rows)")
                            .font(.system(size: theme.sectionSize, weight: .bold))
                            .foregroundColor(palette.secondary)
                    }
                }
            } else {
                WidgetEmptyStateView(
                    reason: state.emptyReason,
                    targetTitle: state.targetTitle,
                    theme: theme,
                    palette: palette
                )
            }

            Spacer(minLength: 0)
        }
        .padding(theme.padding)
    }

    /// Заголовок секции зависит от того, чей день показывается (ТЗ п. 10.1).
    private var sectionTitle: String {
        state.primaryDayContext == .today ? "ДАЛЕЕ СЕГОДНЯ" : "ДАЛЕЕ"
    }

    @ViewBuilder
    private func primary(lesson: WLesson, status: LessonStatus, theme: WidgetTheme) -> some View {
        VStack(alignment: .leading, spacing: theme.lineSpacing) {
            HStack(spacing: 5) {
                LessonStatusView(status: status, theme: theme, palette: palette)
                Spacer(minLength: 0)
                LessonTypeBadge(lesson: lesson, theme: theme, palette: palette)
            }

            HStack(alignment: .firstTextBaseline, spacing: 10) {
                // Крупная аудитория — главный элемент Large (ТЗ п. 10.1).
                ClassroomView(lesson: lesson, theme: theme)
                Spacer(minLength: 4)
                LessonTimeView(
                    lesson: lesson, theme: theme, status: status, palette: palette
                )
            }

            // Название главной пары: в Large место есть — до трёх строк,
            // чтобы длинные дисциплины читались полностью (ТЗ п. 15).
            SubjectTitleView(lesson: lesson, theme: theme, lines: 3, weight: .bold)
        }
    }

    /// Сколько строк списка влезает: главная пара ~110pt, шапка ~20pt,
    /// строка списка ~40pt (время с аудиторией + до двух строк названия).
    private func rowCount(height: CGFloat, theme: WidgetTheme) -> Int {
        let available = height - theme.padding * 2 - 20 - 110 - theme.blockSpacing * 3
        let rowHeight: CGFloat = theme.density == .dense ? 36 : 44
        let fits = Int(available / rowHeight)
        return max(1, min(fits, state.upcomingLessons.count))
    }

    private func theme(for height: CGFloat) -> WidgetTheme {
        let subject = state.primaryLesson?.subject ?? ""
        return WidgetTheme(
            density: WidgetDensity.forSubject(subject, in: height),
            availableHeight: height
        )
    }
}

// MARK: - Extra Large Portrait: информационная панель
//
// Назначение (ТЗ п. 11): самостоятельный интерфейс из трёх смысловых зон, а не
// масштабированный Large. Зоны: A — главная пара, B — остаток выбранного дня,
// C — ближайший следующий учебный день.
//
// Пропорции зон определяются содержимым (ТЗ п. 11.3): если занятий в дне
// больше нет, зона B не рисуется вовсе, а место отдаётся зоне C. Пустые блоки
// и фиксированные высоты запрещены.

struct ExtraLargeScheduleWidgetView: View {
    let state: WidgetPresentationState
    let palette: WidgetPalette

    var body: some View {
        GeometryReader { geo in
            let theme = theme(for: geo.size.height)
            content(theme: theme, height: geo.size.height)
                .frame(width: geo.size.width, height: geo.size.height, alignment: .topLeading)
        }
    }

    @ViewBuilder
    private func content(theme: WidgetTheme, height: CGFloat) -> some View {
        VStack(alignment: .leading, spacing: theme.blockSpacing) {
            if let lesson = state.primaryLesson, let status = state.primaryStatus {
                // Зона A — главная пара.
                zoneA(lesson: lesson, status: status, theme: theme)

                // Зона B — остаток выбранного дня. Не рисуется, если пуста.
                if !state.upcomingLessons.isEmpty {
                    WidgetDivider(palette: palette)
                    zoneB(theme: theme, height: height)
                }

                // Зона C — ближайший следующий учебный день.
                if !state.nextStudyDayLessons.isEmpty, let label = state.nextStudyDay.label {
                    WidgetDivider(palette: palette)
                    zoneC(label: label, theme: theme, height: height)
                }
            } else {
                WidgetEmptyStateView(
                    reason: state.emptyReason,
                    targetTitle: state.targetTitle,
                    theme: theme,
                    palette: palette
                )
            }

            Spacer(minLength: 0)
        }
        .padding(theme.padding)
    }

    // MARK: Зона A

    @ViewBuilder
    private func zoneA(lesson: WLesson, status: LessonStatus, theme: WidgetTheme) -> some View {
        VStack(alignment: .leading, spacing: theme.lineSpacing) {
            WidgetDayHeader(
                text: state.primaryDayLabel ?? "СЕГОДНЯ",
                theme: theme,
                palette: palette,
                trailing: state.freshness.isStale ? "данные устарели" : nil
            )

            HStack(alignment: .firstTextBaseline, spacing: 5) {
                LessonStatusView(status: status, theme: theme, palette: palette)
                Spacer(minLength: 0)
                LessonTypeBadge(lesson: lesson, theme: theme, palette: palette)
            }

            HStack(alignment: .firstTextBaseline, spacing: 12) {
                // Крупная аудитория доминирует (ТЗ п. 11.1).
                ClassroomView(lesson: lesson, theme: theme)
                Spacer(minLength: 8)
                LessonTimeView(
                    lesson: lesson, theme: theme, status: status, palette: palette
                )
            }

            // Полное название: в Extra Large места достаточно, чтобы показать
            // четыре строки даже для самых длинных дисциплин (ТЗ п. 15).
            SubjectTitleView(lesson: lesson, theme: theme, lines: 4, weight: .bold)

            if !lesson.teacher.isEmpty {
                // Преподаватель — P4: в панели место есть, поэтому показываем.
                Text(lesson.teacher)
                    .font(.system(size: theme.sectionSize))
                    .foregroundColor(palette.muted)
                    .lineLimit(1)
            }
        }
    }

    // MARK: Зона B

    @ViewBuilder
    private func zoneB(theme: WidgetTheme, height: CGFloat) -> some View {
        VStack(alignment: .leading, spacing: theme.rowSpacing) {
            WidgetSectionHeader(
                text: state.primaryDayContext == .today ? "ДАЛЕЕ СЕГОДНЯ" : "ДАЛЕЕ",
                theme: theme,
                palette: palette
            )
            ForEach(Array(state.upcomingLessons.prefix(rowsForZoneB(height: height, theme: theme))), id: \.id) { item in
                CompactLessonRow(lesson: item, theme: theme, palette: palette)
            }
        }
    }

    // MARK: Зона C

    @ViewBuilder
    private func zoneC(label: String, theme: WidgetTheme, height: CGFloat) -> some View {
        VStack(alignment: .leading, spacing: theme.rowSpacing) {
            WidgetSectionHeader(
                text: label,
                theme: theme,
                palette: palette
            )
            // ТЗ п. 11.1: 1–3 занятия следующего дня, если позволяет место.
            ForEach(Array(state.nextStudyDayLessons.prefix(rowsForZoneC(height: height, theme: theme))), id: \.id) { item in
                CompactLessonRow(lesson: item, theme: theme, palette: palette)
            }
        }
    }

    // MARK: Расчёт вместимости

    /// Зона B получает оставшееся место после A и (если есть) C.
    /// Оценка по высоте: зона A ~170pt, заголовок зоны C ~20pt,
    /// строка списка ~38pt. Зона C резервирует до трёх строк, только если
    /// будущий день вообще известен, — иначе всё место отдаётся зоне B.
    private func rowsForZoneB(height: CGFloat, theme: WidgetTheme) -> Int {
        let rowHeight: CGFloat = theme.density == .dense ? 32 : 38
        var reserved: CGFloat = theme.padding * 2 + 170 + 20 * 2 + theme.blockSpacing * 3
        if !state.nextStudyDayLessons.isEmpty {
            reserved += min(CGFloat(state.nextStudyDayLessons.count), 3) * rowHeight + 20
        }
        let available = height - reserved
        let fits = Int(available / rowHeight)
        return max(1, min(fits, state.upcomingLessons.count))
    }

    private func rowsForZoneC(height: CGFloat, theme: WidgetTheme) -> Int {
        let rowHeight: CGFloat = theme.density == .dense ? 32 : 38
        // Не даём зоне C съесть место у текущего дня: максимум три строки.
        let fits = Int((height * 0.25) / rowHeight)
        return max(1, min(fits, 3))
    }

    private func theme(for height: CGFloat) -> WidgetTheme {
        let subject = state.primaryLesson?.subject ?? ""
        return WidgetTheme(
            density: WidgetDensity.forSubject(subject, in: height),
            availableHeight: height
        )
    }
}

// MARK: - Сегодня/завтра в подписи

extension DataFreshness {
    var isStale: Bool {
        if case .stale = self { return true }
        return false
    }
}