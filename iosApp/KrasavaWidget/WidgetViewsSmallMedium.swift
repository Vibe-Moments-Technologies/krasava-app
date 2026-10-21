import SwiftUI

// MARK: - Small: одна активная пара (ТЗ п. 4)
//
// Назначение: только активная пара. Никаких следующих занятий, преподавателя,
// списков дней и информационных блоков за пределами главной пары.
//
// Структура строго в таком порядке (ТЗ п. 4.2):
//   A. Header (статус + бейдж типа)
//   B. Classroom (основной визуальный акцент, 20–26 pt)
//   C. LessonTime (одной строкой, 14–16 pt)
//   D. Subject (максимально полно, 2–3 строки)

struct SmallScheduleWidgetView: View {
    let state: WidgetPresentationState
    let palette: WidgetPalette

    var body: some View {
        GeometryReader { geo in
            let density = WidgetDensity.forHeight(geo.size.height)
            let typography = WidgetTypography(density: density)
            content(density: density, typography: typography)
                .frame(width: geo.size.width, height: geo.size.height, alignment: .topLeading)
        }
    }

    @ViewBuilder
    private func content(density: WidgetDensity, typography: WidgetTypography) -> some View {
        if let lesson = state.primaryLesson, let status = state.primaryStatus {
            VStack(alignment: .leading, spacing: 0) {
                header(lesson: lesson, status: status, density: density, typography: typography)

                // Отступы из ТЗ п. 4.3: 8 после Header, 6 после Classroom,
                // 6 после Time. При нехватке высоты сокращаются первыми.
                Spacer().frame(height: density >= .regular ? Space.s8 : Space.s6)

                ClassroomLabel(
                    lesson: lesson,
                    palette: palette,
                    size: typography.room(20...26),
                    weight: .bold
                )
                .layoutPriority(3)

                Spacer().frame(height: Space.s6)

                LessonTimeLabel(
                    lesson: lesson,
                    palette: palette,
                    size: typography.primaryTime(14...16),
                    weight: .semibold
                )
                .layoutPriority(2)

                Spacer().frame(height: Space.s6)

                // Пункт D: название максимально полно. Единственный элемент,
                // которому разрешено занять остаток высоты (ТЗ п. 4.4).
                Text(lesson.subject)
                    .font(.system(size: typography.subject(11...13), weight: .medium))
                    .foregroundColor(palette.subjectText)
                    .lineLimit(density.subjectLines)
                    .minimumScaleFactor(0.85)
                    .fixedSize(horizontal: false, vertical: true)
                    .layoutPriority(1)

                // Композиция прижата кверху: больших Spacer'ов нет, небольшое
                // естественное пустое пространство снизу допустимо (п. 4.4).
                Spacer(minLength: 0)
            }
        } else {
            WidgetEmptyStateView(
                reason: state.emptyReason,
                targetTitle: state.targetTitle,
                palette: palette,
                compact: true
            )
        }
    }

    /// Блок A. Слева статус, справа компактный тип. Если активная пара не
    /// сегодня, вместо бейджа показывается дата: ТЗ п. 4.2 требует контекст
    /// даты и запрещает дублировать его внизу.
    @ViewBuilder
    private func header(
        lesson: WLesson,
        status: LessonStatus,
        density: WidgetDensity,
        typography: WidgetTypography
    ) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: Space.s6) {
            // На Small обратного отсчёта нет: место дороже, точное время
            // информативнее (дополнение владельца).
            LessonStatusLabel(
                status: status,
                palette: palette,
                size: typography.status(10...11),
                allowCountdown: false
            )

            Spacer(minLength: Space.s4)

            if state.primaryDay.isFuture, let dayLabel = state.primaryDay.fullLabel {
                Text(dayLabel)
                    .font(.system(size: typography.caption, weight: .semibold))
                    .foregroundColor(palette.mutedText)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            } else if density >= .compact {
                LessonTypeBadge(lesson: lesson, palette: palette, size: typography.badge)
            }
        }
    }
}

// MARK: - Medium: активная + 1–2 следующие (ТЗ п. 5)
//
// Компоновка по ТЗ п. 5.2: горизонтальное разделение. Слева активная пара
// (55–60% ширины), справа следующие занятия (40–45%) под общим заголовком
// «ДАЛЕЕ». Между зонами тонкий разделитель.
//
// Количество следующих пар выбирается по фактическому месту (ТЗ п. 5.3):
// Mode 0 — только активная, Mode 1 — одна следующая, Mode 2 — две.

struct MediumScheduleWidgetView: View {
    let state: WidgetPresentationState
    let palette: WidgetPalette

    var body: some View {
        GeometryReader { geo in
            let density = WidgetDensity.forHeight(geo.size.height)
            let typography = WidgetTypography(density: density)
            content(density: density, typography: typography, size: geo.size)
                .frame(width: geo.size.width, height: geo.size.height, alignment: .topLeading)
        }
    }

    @ViewBuilder
    private func content(
        density: WidgetDensity,
        typography: WidgetTypography,
        size: CGSize
    ) -> some View {
        if let lesson = state.primaryLesson, let status = state.primaryStatus {
            let rows = upcomingRows(density: density, size: size)
            if rows == 0 {
                // Mode 0: других занятий дня нет — главный блок на всю ширину.
                primary(lesson: lesson, status: status, density: density, typography: typography)
            } else {
                HStack(alignment: .top, spacing: Space.s10) {
                    primary(lesson: lesson, status: status, density: density, typography: typography)
                        // Левая зона 55–60%: главная пара доминирует.
                        .frame(width: primaryWidth(of: size, rows: rows), alignment: .topLeading)

                    WidgetDivider(palette: palette)
                        .frame(width: 1)
                        .frame(maxHeight: .infinity)

                    upcoming(
                        lessons: Array(state.upcomingLessons.prefix(rows)),
                        density: density,
                        typography: typography
                    )
                    .frame(maxWidth: .infinity, alignment: .topLeading)
                }
            }
        } else {
            WidgetEmptyStateView(
                reason: state.emptyReason,
                targetTitle: state.targetTitle,
                palette: palette
            )
        }
    }

    /// Сколько следующих пар влезает. Считается по фактическому размеру,
    /// а не выводится «на удачу» (ТЗ п. 5.3).
    private func upcomingRows(density: WidgetDensity, size: CGSize) -> Int {
        let available = state.upcomingLessons.count
        guard available > 0 else { return 0 }
        // Заголовок «ДАЛЕЕ» ≈ 16 pt, запись ≈ rowHeight.
        let budget = size.height - Space.s16 - 16
        let fits = Int(budget / density.rowHeight)
        // Максимум две (ТЗ п. 5.3). Если две не помещаются без ухудшения
        // основных данных — одна.
        return max(0, min(min(fits, 2), available))
    }

    /// Левая зона: 58% при двух следующих парах, 55% при одной — правая зона
    /// должна уметь сокращаться (ТЗ п. 9.2).
    private func primaryWidth(of size: CGSize, rows: Int) -> CGFloat {
        size.width * (rows >= 2 ? 0.58 : 0.55)
    }

    // MARK: Левая зона

    @ViewBuilder
    private func primary(
        lesson: WLesson,
        status: LessonStatus,
        density: WidgetDensity,
        typography: WidgetTypography
    ) -> some View {
        VStack(alignment: .leading, spacing: density.lineSpacing) {
            // Статус и тип. На Medium отсчёт уместен: место есть, и он
            // не отнимает у P0-данных (дополнение владельца).
            HStack(alignment: .firstTextBaseline, spacing: Space.s6) {
                LessonStatusLabel(
                    status: status,
                    palette: palette,
                    size: typography.status(10...11),
                    allowCountdown: true
                )
                Spacer(minLength: Space.s4)
                LessonTypeBadge(lesson: lesson, palette: palette, size: typography.badge)
            }

            // Аудитория — главный акцент левой зоны.
            ClassroomLabel(
                lesson: lesson,
                palette: palette,
                size: typography.room(16...20),
                weight: .bold
            )
            .padding(.top, Space.s4)

            LessonTimeLabel(
                lesson: lesson,
                palette: palette,
                size: typography.primaryTime(12...15),
                weight: .semibold
            )

            Text(lesson.subject)
                .font(.system(size: typography.subject(11...13), weight: .medium))
                .foregroundColor(palette.subjectText)
                .lineLimit(density.subjectLines)
                .minimumScaleFactor(0.85)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, Space.s2)
                .layoutPriority(1)

            // Вторичные метаданные: преподаватель и число групп. Скрываются
            // первыми при нехватке места (ТЗ п. 5.6).
            if density.showsTeacher {
                TeacherLabel(lesson: lesson, palette: palette, size: typography.teacher)
            }
            if density.showsGroupIndicator {
                MultiGroupIndicator(
                    lesson: lesson,
                    palette: palette,
                    size: typography.teacher - 1,
                    showWord: density >= .regular
                )
            }
        }
    }

    // MARK: Правая зона

    @ViewBuilder
    private func upcoming(
        lessons: [WLesson],
        density: WidgetDensity,
        typography: WidgetTypography
    ) -> some View {
        VStack(alignment: .leading, spacing: density.rowSpacing) {
            // Общий заголовок один раз, а не перед каждой записью (ТЗ п. 5.2).
            WidgetSectionTitle(
                text: "ДАЛЕЕ",
                palette: palette,
                size: typography.caption
            )

            ForEach(Array(lessons.enumerated()), id: \.element.id) { index, lesson in
                if index > 0 {
                    WidgetDivider(palette: palette)
                }
                UpcomingLessonRow(
                    lesson: lesson,
                    palette: palette,
                    density: density,
                    typography: typography
                )
            }
        }
    }
}

// MARK: - Пустые состояния (ТЗ п. 10.4)
//
// «Нет занятий» ≠ «нет данных» ≠ «нет группы». Технические тексты ошибок
// пользователю не показываются, но действие объясняется.

struct WidgetEmptyStateView: View {
    let reason: EmptyReason?
    let targetTitle: String?
    let palette: WidgetPalette
    var compact: Bool = false

    var body: some View {
        VStack(spacing: compact ? Space.s4 : Space.s6) {
            Image(systemName: icon)
                .font(.system(size: compact ? 20 : 24))
                .foregroundColor(iconTint)
            Text(title)
                .font(.system(size: compact ? 13 : 15, weight: .semibold))
                .foregroundColor(palette.subjectText)
                .multilineTextAlignment(.center)
            if let subtitle {
                Text(subtitle)
                    .font(.system(size: compact ? 11 : 12))
                    .foregroundColor(palette.mutedText)
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
        case .badData, .noAppGroup: return palette.activeAccent
        default: return palette.mutedText
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
