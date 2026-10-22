import SwiftUI

// MARK: - Large: день целиком (ТЗ п. 6)
//
// Вертикальное расширение Medium. Показывает подробную активную пару, все
// последующие занятия выбранного дня, которые помещаются, и при наличии
// места — ближайший следующий учебный день отдельным разделом.
//
// Корневой контейнер — VStack(alignment: .leading, spacing: 0): пространство
// задаётся на уровне секций, а не одним общим интервалом (ТЗ п. 6.2).

struct LargeScheduleWidgetView: View {
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
    private func content(density: WidgetDensity, typography: WidgetTypography, size: CGSize) -> some View {
        if let lesson = state.primaryLesson, let status = state.primaryStatus {
            VStack(alignment: .leading, spacing: 0) {
                // Заголовок выбранного учебного дня (ТЗ п. 6.3): приглушённый,
                // 11–12 pt. Дата не должна быть самым крупным элементом.
                HStack(spacing: Space.s6) {
                    WidgetSectionTitle(
                        text: state.headerTitle,
                        palette: palette,
                        size: typography.caption,
                        isDayHeader: true
                    )
                    Spacer(minLength: 0)
                    if state.freshness.isStale {
                        Text("данные устарели")
                            .font(.system(size: typography.caption - 1))
                            .foregroundColor(palette.mutedText)
                            .lineLimit(1)
                    }
                }

                Spacer().frame(height: Space.s8)

                // Главная пара (ТЗ п. 6.4): статус и тип, крупная аудитория
                // с временем, название, метаданные.
                primary(lesson: lesson, status: status, density: density, typography: typography)

                // Раздел следующих занятий — только если они есть.
                if !state.upcomingLessons.isEmpty {
                    Spacer().frame(height: Space.s10)
                    WidgetDivider(palette: palette)
                    Spacer().frame(height: Space.s10)

                    WidgetSectionTitle(
                        text: state.primaryDay.isToday ? "ДАЛЕЕ СЕГОДНЯ" : "ДАЛЕЕ",
                        palette: palette,
                        size: typography.caption
                    )
                    Spacer().frame(height: Space.s6)

                    let rows = upcomingRows(density: density, size: size, typography: typography)
                    ForEach(Array(state.upcomingLessons.prefix(rows).enumerated()), id: \.element.id) { index, item in
                        if index > 0 {
                            Spacer().frame(height: density.rowSpacing)
                        }
                        UpcomingLessonRow(
                            lesson: item,
                            palette: palette,
                            density: density,
                            typography: typography,
                            showsType: density >= .regular
                        )
                    }
                    if state.upcomingLessons.count > rows {
                        Spacer().frame(height: Space.s4)
                        Text("Ещё \(state.upcomingLessons.count - rows)")
                            .font(.system(size: typography.caption))
                            .foregroundColor(palette.mutedText)
                    }
                }

                // Раздел ближайшего следующего учебного дня (ТЗ п. 6.8):
                // только при достаточной высоте для заголовка и хотя бы одной
                // полноценной записи. Одинокий заголовок не выводится.
                if let nextDay = state.nextDays.first,
                   let rows = nextDayRows(nextDay, density: density, size: size, typography: typography),
                   rows > 0 {
                    Spacer().frame(height: Space.s10)
                    WidgetDivider(palette: palette)
                    Spacer().frame(height: Space.s10)

                    WidgetSectionTitle(
                        text: nextDay.label,
                        palette: palette,
                        size: typography.caption,
                        isDayHeader: true
                    )
                    Spacer().frame(height: Space.s6)

                    ForEach(Array(nextDay.lessons.prefix(rows).enumerated()), id: \.element.id) { index, item in
                        if index > 0 {
                            Spacer().frame(height: density.rowSpacing)
                        }
                        UpcomingLessonRow(
                            lesson: item,
                            palette: palette,
                            density: density,
                            typography: typography
                        )
                    }
                }

                Spacer(minLength: 0)
            }
        } else {
            WidgetEmptyStateView(
                reason: state.emptyReason,
                targetTitle: state.targetTitle,
                palette: palette
            )
        }
    }

    // MARK: Главная пара

    /// ТЗ п. 6.4: статус слева, тип справа; ниже крупная аудитория и время;
    /// затем название; затем метаданные. Высота — по содержимому, без
    /// фиксированного frame (ТЗ п. 6.5).
    @ViewBuilder
    private func primary(
        lesson: WLesson,
        status: LessonStatus,
        density: WidgetDensity,
        typography: WidgetTypography
    ) -> some View {
        VStack(alignment: .leading, spacing: density.lineSpacing) {
            HStack(alignment: .firstTextBaseline, spacing: Space.s6) {
                LessonStatusLabel(
                    status: status,
                    palette: palette,
                    size: typography.status(11...12),
                    allowCountdown: true
                )
                Spacer(minLength: Space.s4)
                LessonTypeBadge(lesson: lesson, palette: palette, size: typography.badge)
            }

            // Аудитория и время в одну строку. `ViewThatFits` недоступен на
            // iOS 15 (минимум проекта — 15.3), поэтому при нехватке ширины
            // ужимается аудитория: у ClassroomLabel есть minimumScaleFactor
            // и единый Text, так что кампус не отделяется от номера.
            HStack(alignment: .firstTextBaseline, spacing: Space.s10) {
                ClassroomLabel(
                    lesson: lesson,
                    palette: palette,
                    size: typography.room(24...30),
                    weight: .bold
                )
                .layoutPriority(1)
                Spacer(minLength: Space.s4)
                LessonTimeLabel(
                    lesson: lesson,
                    palette: palette,
                    size: typography.primaryTime(17...20),
                    weight: .semibold
                )
            }
            .padding(.top, Space.s2)

            Text(lesson.subject)
                .font(.system(size: typography.subject(13...15), weight: .medium))
                .foregroundColor(palette.subjectText)
                .lineLimit(density.subjectLines)
                .minimumScaleFactor(0.85)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, Space.s2)
                .layoutPriority(1)

            // Метаданные в одну строку: преподаватель и индикатор групп.
            // Место под отсутствующего преподавателя не резервируется.
            //
            // Условие `density.showsTeacher` здесь не применяется: данные
            // преподавателя доходят до вью (проверено по всей цепочке), и в
            // Large при высоте ~354 pt плотность всегда regular, так что
            // ограничение ничего не отсекало бы. Оставляем его только для
            // групп, чтобы не ломать приоритет скрытия из ТЗ п. 5.6.
            let hasTeacher = lesson.teacherDisplay != nil
            let hasGroups = lesson.isShared
            if hasTeacher || hasGroups {
                HStack(spacing: Space.s6) {
                    if hasTeacher {
                        TeacherLabel(lesson: lesson, palette: palette, size: typography.teacher)
                    }
                    if hasTeacher && hasGroups {
                        Text("·")
                            .font(.system(size: typography.teacher))
                            .foregroundColor(palette.mutedText)
                    }
                    if hasGroups {
                        MultiGroupIndicator(
                            lesson: lesson,
                            palette: palette,
                            size: typography.teacher - 1,
                            showWord: density >= .regular
                        )
                    }
                    Spacer(minLength: 0)
                }
                .padding(.top, Space.s2)
            }
        }
    }

    // MARK: Расчёт вместимости (ТЗ п. 6.7, 9.4)

    /// Сколько записей выбранного дня влезает.
    ///
    /// Алгоритм: из доступной высоты вычитается главная секция (её высота
    /// зависит от плотности), затем каждая запись добавляется, пока хватает
    /// места. Показываются только полностью помещающиеся записи.
    private func upcomingRows(
        density: WidgetDensity,
        size: CGSize,
        typography: WidgetTypography
    ) -> Int {
        let available = state.upcomingLessons.count
        guard available > 0 else { return 0 }

        // Главная секция: статус + аудитория + название (до 3 строк) + метаданные.
        let primaryHeight: CGFloat = density >= .regular ? 132 : 112
        // Резерв под раздел будущего дня: заголовок + одна запись.
        let nextDayReserve = state.nextDays.isEmpty ? 0 : (density.rowHeight + 28)

        var budget = size.height - primaryHeight - nextDayReserve - 60
        var rows = 0
        while rows < available {
            let cost = density.rowHeight + (rows > 0 ? density.rowSpacing : 0)
            if budget < cost { break }
            budget -= cost
            rows += 1
        }
        return rows
    }

    /// Сколько записей будущего дня влезает в оставшееся место.
    ///
    /// Возвращает nil, если места не хватает даже на одну полноценную запись:
    /// в этом случае раздел не выводится вовсе (ТЗ п. 6.8).
    private func nextDayRows(
        _ day: WidgetDay,
        density: WidgetDensity,
        size: CGSize,
        typography: WidgetTypography
    ) -> Int? {
        let primaryHeight: CGFloat = density >= .regular ? 132 : 112
        // Заголовок + разделители + отступы раздела.
        let sectionChrome: CGFloat = 56
        let usedByUpcoming = CGFloat(state.upcomingLessons.count) * density.rowHeight

        var budget = size.height - primaryHeight - sectionChrome - usedByUpcoming - 28
        guard budget >= density.rowHeight else { return nil }

        var rows = 0
        for _ in day.lessons {
            if budget < density.rowHeight { break }
            budget -= density.rowHeight
            rows += 1
        }
        return rows > 0 ? rows : nil
    }
}

// MARK: - Extra Large Portrait: гибридная панель (ТЗ п. 7)
//
// Три смысловые зоны в фиксированном порядке: активная пара, остаток
// выбранного дня, следующие учебные дни. Высота секций зависит от данных
// (ТЗ п. 7.6), а не от фиксированных пропорций: если зона B пуста, её место
// занимает зона C.
//
// Примечание по доступности API: `systemExtraLargePortrait` в SDK iOS
// объявлен как `@available(iOS, unavailable)` — на iPhone такого семейства
// нет. Композиция реализована и используется для iPad-размера
// `systemExtraLarge`; на iPhone виджет её не покажет, но логика готова.

struct ExtraLargeScheduleWidgetView: View {
    let state: WidgetPresentationState
    let palette: WidgetPalette

    var body: some View {
        GeometryReader { geo in
            let density = WidgetDensity.forHeight(geo.size.height)
            let typography = WidgetTypography(density: density)
            let isLandscape = geo.size.width > geo.size.height * 1.15
            Group {
                if isLandscape {
                    landscape(density: density, typography: typography, size: geo.size)
                } else {
                    portrait(density: density, typography: typography, size: geo.size)
                }
            }
            .frame(width: geo.size.width, height: geo.size.height, alignment: .topLeading)
        }
    }

    // MARK: Вертикальная панель

    @ViewBuilder
    private func portrait(density: WidgetDensity, typography: WidgetTypography, size: CGSize) -> some View {
        if let lesson = state.primaryLesson, let status = state.primaryStatus {
            VStack(alignment: .leading, spacing: 0) {
                zoneA(lesson: lesson, status: status, density: density, typography: typography)

                // Зона B — остаток выбранного дня. Не рисуется, если пуста.
                if !state.upcomingLessons.isEmpty {
                    sectionSeparator()
                    zoneB(density: density, typography: typography, size: size)
                }

                // Зона C — следующие учебные дни: до двух, каждый со своим
                // заголовком (ТЗ п. 7.5).
                if !state.nextDays.isEmpty {
                    sectionSeparator()
                    zoneC(density: density, typography: typography, size: size)
                }

                Spacer(minLength: 0)
            }
        } else {
            WidgetEmptyStateView(
                reason: state.emptyReason,
                targetTitle: state.targetTitle,
                palette: palette
            )
        }
    }

    // MARK: Горизонтальная панель (iPad, ТЗ п. 1.2)

    /// Три колонки: активная пара — остаток дня — следующий учебный день.
    /// Колонки без данных не рисуются.
    @ViewBuilder
    private func landscape(density: WidgetDensity, typography: WidgetTypography, size: CGSize) -> some View {
        if let lesson = state.primaryLesson, let status = state.primaryStatus {
            HStack(alignment: .top, spacing: Space.s12) {
                // Активная пара доминирует: ей отдаётся 42% ширины.
                zoneA(lesson: lesson, status: status, density: density, typography: typography)
                    .frame(width: size.width * 0.42, alignment: .topLeading)

                if !state.upcomingLessons.isEmpty {
                    columnSeparator()
                    zoneB(density: density, typography: typography, size: size)
                        .frame(maxWidth: .infinity, alignment: .topLeading)
                }

                if let nextDay = state.nextDays.first {
                    columnSeparator()
                    VStack(alignment: .leading, spacing: density.rowSpacing) {
                        WidgetSectionTitle(
                            text: nextDay.label,
                            palette: palette,
                            size: typography.caption,
                            isDayHeader: true
                        )
                        ForEach(Array(nextDay.lessons.prefix(3).enumerated()), id: \.element.id) { index, item in
                            if index > 0 { Spacer().frame(height: Space.s4) }
                            UpcomingLessonRow(
                                lesson: item,
                                palette: palette,
                                density: density,
                                typography: typography
                            )
                        }
                    }
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

    // MARK: Зона A — активная пара (ТЗ п. 7.3)

    @ViewBuilder
    private func zoneA(
        lesson: WLesson,
        status: LessonStatus,
        density: WidgetDensity,
        typography: WidgetTypography
    ) -> some View {
        VStack(alignment: .leading, spacing: density.lineSpacing) {
            // Заголовок даты: «СЕГОДНЯ · СРЕДА» или «ПТ, 23 ОКТ».
            WidgetSectionTitle(
                text: state.headerTitle,
                palette: palette,
                size: typography.caption,
                isDayHeader: true
            )

            HStack(alignment: .firstTextBaseline, spacing: Space.s6) {
                LessonStatusLabel(
                    status: status,
                    palette: palette,
                    size: typography.status(12...13),
                    allowCountdown: true
                )
                Spacer(minLength: Space.s4)
                LessonTypeBadge(lesson: lesson, palette: palette, size: typography.badge)
            }
            .padding(.top, Space.s4)

            ClassroomLabel(
                lesson: lesson,
                palette: palette,
                size: typography.room(27...34),
                weight: .bold
            )
            .padding(.top, Space.s4)

            LessonTimeLabel(
                lesson: lesson,
                palette: palette,
                size: typography.primaryTime(18...22),
                weight: .semibold
            )

            Text(lesson.subject)
                .font(.system(size: typography.subject(14...16), weight: .medium))
                .foregroundColor(palette.subjectText)
                .lineLimit(density.subjectLines + 1)
                .minimumScaleFactor(0.85)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, Space.s4)

            // Метаданные зоны A: преподаватель и число групп.
            //
            // Тот же принцип, что в Large: данные доходят до вью, поэтому
            // ограничение плотности не применяется — при высоте панели
            // плотность всегда regular.
            if let _ = lesson.teacherDisplay, lesson.isShared {
                HStack(spacing: Space.s6) {
                    TeacherLabel(lesson: lesson, palette: palette, size: typography.teacher)
                    Text("·")
                        .font(.system(size: typography.teacher))
                        .foregroundColor(palette.mutedText)
                    MultiGroupIndicator(
                        lesson: lesson,
                        palette: palette,
                        size: typography.teacher,
                        showWord: true
                    )
                    Spacer(minLength: 0)
                }
                .padding(.top, Space.s2)
            } else if let _ = lesson.teacherDisplay {
                TeacherLabel(lesson: lesson, palette: palette, size: typography.teacher)
                    .padding(.top, Space.s2)
            } else if lesson.isShared {
                MultiGroupIndicator(
                    lesson: lesson,
                    palette: palette,
                    size: typography.teacher,
                    showWord: true
                )
                .padding(.top, Space.s2)
            }
        }
    }

    // MARK: Зона B — остаток дня (ТЗ п. 7.4)

    @ViewBuilder
    private func zoneB(density: WidgetDensity, typography: WidgetTypography, size: CGSize) -> some View {
        VStack(alignment: .leading, spacing: density.rowSpacing) {
            WidgetSectionTitle(
                text: state.primaryDay.isToday ? "ДАЛЕЕ СЕГОДНЯ" : "ДАЛЕЕ",
                palette: palette,
                size: typography.caption
            )

            let rows = zoneBRows(density: density, size: size)
            ForEach(Array(state.upcomingLessons.prefix(rows).enumerated()), id: \.element.id) { index, item in
                if index > 0 {
                    Spacer().frame(height: Space.s4)
                }
                UpcomingLessonRow(
                    lesson: item,
                    palette: palette,
                    density: density,
                    typography: typography,
                    showsType: density >= .regular
                )
            }
        }
    }

    // MARK: Зона C — следующие учебные дни (ТЗ п. 7.5)

    @ViewBuilder
    private func zoneC(density: WidgetDensity, typography: WidgetTypography, size: CGSize) -> some View {
        VStack(alignment: .leading, spacing: density.rowSpacing) {
            ForEach(Array(state.nextDays.prefix(2).enumerated()), id: \.element.date) { dayIndex, day in
                if dayIndex > 0 {
                    Spacer().frame(height: Space.s6)
                }
                // Каждый день обязан иметь собственный заголовок: смешивать
                // занятия разных дней под одной подписью нельзя (ТЗ п. 7.5).
                WidgetSectionTitle(
                    text: day.label,
                    palette: palette,
                    size: typography.caption,
                    isDayHeader: true
                )
                let limit = dayIndex == 0 ? 3 : 1
                ForEach(Array(day.lessons.prefix(limit).enumerated()), id: \.element.id) { index, item in
                    if index > 0 {
                        Spacer().frame(height: Space.s4)
                    }
                    UpcomingLessonRow(
                        lesson: item,
                        palette: palette,
                        density: density,
                        typography: typography
                    )
                }
            }
        }
    }

    // MARK: Расчёты и украшения

    /// Сколько записей остатка дня влезает в вертикальной панели.
    ///
    /// ТЗ п. 7.6: приоритет — активная пара, затем остаток дня, затем
    /// будущие дни. Поэтому будущим дням резервируется минимум, а остаток
    /// отдаётся зоне B.
    private func zoneBRows(density: WidgetDensity, size: CGSize) -> Int {
        let available = state.upcomingLessons.count
        guard available > 0 else { return 0 }

        let zoneAHeight: CGFloat = density >= .regular ? 190 : 165
        let zoneCHeight: CGFloat = state.nextDays.isEmpty ? 0 : 140
        var budget = size.height - zoneAHeight - zoneCHeight - 40

        var rows = 0
        while rows < available {
            let cost = density.rowHeight + (rows > 0 ? Space.s4 : 0)
            if budget < cost { break }
            budget -= cost
            rows += 1
        }
        return rows
    }

    @ViewBuilder
    private func sectionSeparator() -> some View {
        VStack(spacing: 0) {
            Spacer().frame(height: Space.s10)
            WidgetDivider(palette: palette)
            Spacer().frame(height: Space.s10)
        }
    }

    @ViewBuilder
    private func columnSeparator() -> some View {
        WidgetDivider(palette: palette)
            .frame(width: 1)
            .frame(maxHeight: .infinity)
    }
}
