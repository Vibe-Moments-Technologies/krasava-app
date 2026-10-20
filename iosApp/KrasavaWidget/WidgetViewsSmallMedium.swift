import SwiftUI

// MARK: - Small: одна актуальная пара
//
// Назначение (ТЗ п. 8): максимально быстрый доступ к одной паре. Пользователь
// должен с одного взгляда различить аудиторию, время и актуальность.
//
// Порядок блоков продиктован приоритетами: сначала то, что отвечает на вопрос
// «куда идти» (аудитория), затем «когда» (время), затем «что» (название).
// Группа, преподаватель и декоративные иконки не выводятся — они не нужны
// на домашнем экране и отнимают место у P0.

struct SmallScheduleWidgetView: View {
    let state: WidgetPresentationState
    let palette: WidgetPalette

    var body: some View {
        GeometryReader { geo in
            let theme = theme(for: geo.size.height)
            content(theme: theme)
                .frame(width: geo.size.width, height: geo.size.height, alignment: .topLeading)
        }
    }

    @ViewBuilder
    private func content(theme: WidgetTheme) -> some View {
        VStack(alignment: .leading, spacing: theme.blockSpacing) {
            if let lesson = state.primaryLesson, let status = state.primaryStatus {
                header(status: status, theme: theme)

                // Аудитория — главный элемент, поэтому сразу после статуса.
                ClassroomView(lesson: lesson, theme: theme)

                LessonTimeView(
                    lesson: lesson, theme: theme, status: status, palette: palette
                )

                SubjectTitleView(
                    lesson: lesson, theme: theme,
                    lines: subjectLines(for: theme),
                    weight: .semibold
                )
                .layoutPriority(1)

                Spacer(minLength: 0)

                footer(lesson: lesson, theme: theme)
            } else {
                emptyState(theme: theme)
            }
        }
        .padding(theme.padding)
    }

    /// Статус без обратного отсчёта: на Small место дороже (дополнение владельца).
    @ViewBuilder
    private func header(status: LessonStatus, theme: WidgetTheme) -> some View {
        HStack(spacing: 5) {
            LessonStatusView(
                status: status, theme: theme, palette: palette, allowCountdown: false
            )
            Spacer(minLength: 0)
            // По ТЗ п. 8.3 при очень длинном названии бейдж типа убирается
            // первым — здесь он есть только если статус не занят датой.
            if state.primaryDayContext == .today, let lesson = state.primaryLesson {
                LessonTypeBadge(lesson: lesson, theme: theme, palette: palette)
            }
        }
    }

    /// Дата будущего дня обязательна (ТЗ п. 2, 16): «ДАЛЕЕ» без даты для
    /// завтрашней пары недопустимо.
    @ViewBuilder
    private func footer(lesson: WLesson, theme: WidgetTheme) -> some View {
        if let dayLabel = state.primaryDayLabel, state.primaryDayContext.isFuture {
            Text(dayLabel)
                .font(.system(size: theme.sectionSize, weight: .heavy))
                .foregroundColor(palette.secondary)
                .lineLimit(1)
        } else if !lesson.teacher.isEmpty {
            // Преподаватель — P4: показывается, только если день уже подписан
            // не был и место осталось.
            Text(lesson.teacher)
                .font(.system(size: theme.sectionSize))
                .foregroundColor(palette.muted)
                .lineLimit(1)
        }
    }

    /// Сколько строк дать названию: длинные дисциплины получают третью строку.
    private func subjectLines(for theme: WidgetTheme) -> Int {
        theme.density == .dense ? 3 : 2
    }

    private func emptyState(theme: WidgetTheme) -> some View {
        WidgetEmptyStateView(
            reason: state.emptyReason,
            targetTitle: state.targetTitle,
            theme: theme,
            palette: palette,
            compact: true
        )
    }

    private func theme(for height: CGFloat) -> WidgetTheme {
        let subject = state.primaryLesson?.subject ?? ""
        return WidgetTheme(
            density: WidgetDensity.forSubject(subject, in: height),
            availableHeight: height
        )
    }
}

// MARK: - Medium: текущая + следующая
//
// Назначение (ТЗ п. 9): актуальная пара и следующая после неё.
//
// Из двух вариантов компоновки выбран вертикальный (B): главная пара крупно
// сверху, следующая — компактной строкой снизу. Горизонтальные зоны (A) дают
// главной паре всего ~65% ширины medium, и длинные названия вроде
// «Организационное и правовое обеспечение информационной безопасности»
// обрезаются, тогда как вертикальная раскладка отдаёт названию всю ширину.

struct MediumScheduleWidgetView: View {
    let state: WidgetPresentationState
    let palette: WidgetPalette

    var body: some View {
        GeometryReader { geo in
            let theme = theme(for: geo.size.height)
            content(theme: theme)
                .frame(width: geo.size.width, height: geo.size.height, alignment: .topLeading)
        }
    }

    @ViewBuilder
    private func content(theme: WidgetTheme) -> some View {
        VStack(alignment: .leading, spacing: theme.blockSpacing) {
            if let lesson = state.primaryLesson, let status = state.primaryStatus {
                primary(lesson: lesson, status: status, theme: theme)

                if let next = state.upcomingLessons.first {
                    // ТЗ п. 9.4: пустую зону «Далее» без данных не создаём.
                    WidgetDivider(palette: palette)
                    secondary(next: next, theme: theme)
                }

                Spacer(minLength: 0)
            } else {
                WidgetEmptyStateView(
                    reason: state.emptyReason,
                    targetTitle: state.targetTitle,
                    theme: theme,
                    palette: palette
                )
            }
        }
        .padding(theme.padding)
    }

    /// Главная пара: статус, аудитория, время, название. Здесь отсчёт уместен —
    /// место есть, и он не отнимает у P0 (дополнение владельца).
    @ViewBuilder
    private func primary(lesson: WLesson, status: LessonStatus, theme: WidgetTheme) -> some View {
        VStack(alignment: .leading, spacing: theme.lineSpacing) {
            HStack(spacing: 5) {
                LessonStatusView(status: status, theme: theme, palette: palette)
                if state.primaryDayContext.isFuture, let dayLabel = state.primaryDayLabel {
                    // Дата будущего дня прямо в статусной строке: пара не сегодня.
                    Text("· \(dayLabel)")
                        .font(.system(size: theme.statusSize, weight: .bold))
                        .foregroundColor(palette.secondary)
                        .lineLimit(1)
                }
                Spacer(minLength: 0)
                LessonTypeBadge(lesson: lesson, theme: theme, palette: palette)
            }

            HStack(alignment: .firstTextBaseline, spacing: 8) {
                LessonTimeView(
                    lesson: lesson, theme: theme, status: status, palette: palette
                )
                Spacer(minLength: 4)
                ClassroomView(lesson: lesson, theme: theme, compact: true)
                    // Аудитория приоритетнее названия: она P0, название P1.
                    .layoutPriority(1)
            }

            SubjectTitleView(lesson: lesson, theme: theme, lines: 2)
                .layoutPriority(2)
        }
    }

    /// Следующая пара: время · аудитория · сокращённое название.
    @ViewBuilder
    private func secondary(next: WLesson, theme: WidgetTheme) -> some View {
        VStack(alignment: .leading, spacing: theme.lineSpacing) {
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text("ДАЛЕЕ")
                    .font(.system(size: theme.sectionSize, weight: .heavy))
                    .foregroundColor(palette.secondary)
                LessonTimeView(
                    lesson: next, theme: theme, status: nil, palette: palette, compact: true
                )
                Spacer(minLength: 2)
                ClassroomView(lesson: next, theme: theme, compact: true)
            }
            // Название следующей пары — вторичное: одна строка с усадкой
            // (ТЗ п. 9.2: второстепенная зона должна уметь сокращаться).
            SubjectTitleView(lesson: next, theme: theme, compact: true, lines: 1, weight: .regular)
                .foregroundColor(palette.secondary)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(
            "Далее \(next.start), \(next.subject), аудитория \(next.roomDisplay)"
        )
    }

    private func theme(for height: CGFloat) -> WidgetTheme {
        let subject = state.primaryLesson?.subject ?? ""
        return WidgetTheme(
            density: WidgetDensity.forSubject(subject, in: height),
            availableHeight: height
        )
    }
}

// MARK: - Пустые состояния
//
// ТЗ п. 27: состояния различаются и не подменяют друг друга техническими
// текстами. «Нет занятий» ≠ «нет данных» ≠ «нет группы».

struct WidgetEmptyStateView: View {
    let reason: EmptyReason?
    let targetTitle: String?
    let theme: WidgetTheme
    let palette: WidgetPalette
    var compact: Bool = false

    var body: some View {
        VStack(spacing: compact ? 4 : 6) {
            Image(systemName: icon)
                .font(.system(size: compact ? 20 : 24))
                .foregroundColor(iconTint)
            Text(title)
                .font(.system(size: theme.subjectSize - 1, weight: .bold))
                .multilineTextAlignment(.center)
            if let subtitle {
                Text(subtitle)
                    .font(.system(size: theme.sectionSize))
                    .foregroundColor(palette.secondary)
                    .multilineTextAlignment(.center)
                    .lineLimit(compact ? 2 : 3)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(title). \(subtitle ?? "")")
    }

    private var icon: String {
        switch reason {
        case .noLessons: return "calendar"
        case .noTarget: return "slider.horizontal.3"
        case .noData: return "calendar.badge.plus"
        case .badData: return "exclamationmark.triangle"
        case .noAppGroup: return "exclamationmark.shield"
        case nil: return "calendar"
        }
    }

    private var iconTint: Color {
        switch reason {
        case .badData, .noAppGroup: return palette.upcomingAccent
        default: return palette.secondary
        }
    }

    private var title: String {
        switch reason {
        case .noLessons: return "Нет занятий"
        case .noTarget: return "Выберите группу"
        case .noData: return "Нет данных"
        case .badData: return "Данные не читаются"
        case .noAppGroup: return "Нет доступа к данным"
        case nil: return "Нет занятий"
        }
    }

    /// Подсказка не должна раскрывать технические детали (ТЗ п. 27), но должна
    /// объяснять действие: как исправить.
    private var subtitle: String? {
        switch reason {
        case .noLessons:
            return targetTitle.map { "По расписанию «\($0)»" } ?? "В загруженном расписании"
        case .noTarget:
            return "Долгое нажатие → «Изменить виджет» → Группа"
        case .noData:
            return "Откройте приложение и добавьте расписание"
        case .badData:
            return "Откройте приложение, чтобы обновить данные"
        case .noAppGroup:
            return "Приложение нужно переустановить"
        case nil:
            return nil
        }
    }
}