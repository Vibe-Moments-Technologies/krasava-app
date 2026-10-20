import SwiftUI

// MARK: - Общие компоненты виджетов
//
// Один набор деталей на все четыре размера (ТЗ п. 17): аудитория, время,
// название, бейдж типа, статус всегда выглядят и усекаются одинаково.
// Представления не решают, что важнее — это уже решено здесь и в токенах.

/// Аудитория и корпус — P0: никогда не обрезается номер (ТЗ п. 2).
///
/// Номер крупный (главный элемент иерархии), корпус — рядом, мельче.
/// Если корпуса нет, строка не резервирует под него место.
struct ClassroomView: View {
    let lesson: WLesson
    let theme: WidgetTheme
    /// Компактный вариант — для строк списка, где аудитория не главная.
    var compact: Bool = false

    var body: some View {
        let number = lesson.roomNumber
        let building = lesson.buildingLabel

        if number.isEmpty {
            // ТЗ п. 7: пустоты и «— (—)» быть не должно — показываем подпись.
            Text(lesson.roomDisplay)
                .font(.system(size: compact ? theme.rowRoomSize : theme.buildingSize,
                              weight: .semibold))
                .foregroundColor(.secondary)
                .lineLimit(1)
        } else if compact {
            HStack(alignment: .firstTextBaseline, spacing: 3) {
                Text(number)
                    .font(.system(size: theme.rowRoomSize, weight: .bold))
                if !building.isEmpty {
                    Text(building)
                        .font(.system(size: theme.rowRoomSize - 2))
                        .foregroundColor(.secondary)
                }
            }
            // Номер аудитории не усекается: он P0, вторичное ужимается первым.
            .lineLimit(1)
            .minimumScaleFactor(0.85)
        } else {
            VStack(alignment: .leading, spacing: 0) {
                Text(number)
                    .font(.system(size: theme.roomSize, weight: .bold))
                    .lineLimit(1)
                    // Усадка вместо обрезки: «341» и «258а» должны читаться целиком.
                    .minimumScaleFactor(0.7)
                if !building.isEmpty {
                    Text(building)
                        .font(.system(size: theme.buildingSize, weight: .medium))
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }
            }
        }
    }
}

/// Время занятия — P0: всегда одной строкой, не разрывается (ТЗ п. 8.2).
struct LessonTimeView: View {
    let lesson: WLesson
    let theme: WidgetTheme
    let status: LessonStatus?
    let palette: WidgetPalette
    /// Крупный вариант — главная пара, компактный — строка списка.
    var compact: Bool = false

    var body: some View {
        Text("\(lesson.start)–\(lesson.end)")
            .font(.system(
                size: compact ? theme.rowTimeSize : theme.timeSize,
                weight: compact ? .semibold : .bold
            ))
            // Моноширинные цифры: строки списка не «пляшут» по ширине.
            .monospacedDigit()
            .foregroundColor(status?.isCurrent == true ? palette.currentAccent : .primary)
            .lineLimit(1)
            .fixedSize(horizontal: true, vertical: false)
    }
}

/// Название дисциплины — P1: максимально полно, усадка вместо многоточия.
struct SubjectTitleView: View {
    let lesson: WLesson
    let theme: WidgetTheme
    var compact: Bool = false
    var lines: Int = 2
    var weight: Font.Weight = .semibold

    var body: some View {
        Text(lesson.subject)
            .font(.system(size: compact ? theme.rowSubjectSize : theme.subjectSize, weight: weight))
            .lineLimit(lines)
            // 0.85 читается; бездумные 0.3 по ТЗ п. 15 запрещены.
            .minimumScaleFactor(0.85)
            .fixedSize(horizontal: false, vertical: true)
    }
}

/// Бейдж типа занятия — P3: компактная метка.
struct LessonTypeBadge: View {
    let lesson: WLesson
    let theme: WidgetTheme
    let palette: WidgetPalette

    var body: some View {
        let tint = palette.typeColor(lesson.type)
        Text(lesson.type)
            .font(.system(size: theme.badgeSize, weight: .bold))
            .foregroundColor(tint)
            .padding(.horizontal, 4)
            .padding(.vertical, 1)
            .background(tint.opacity(0.15))
            .clipShape(Capsule())
            .lineLimit(1)
            // Тип — второстепенная метка: она уходит первой при нехватке места.
            .layoutPriority(-1)
    }
}

/// Статус пары: «СЕЙЧАС», «ДАЛЕЕ», «ЧЕРЕЗ 25 МИН».
///
/// По дополнению владельца обратный отсчёт не выводится на Small — там
/// место дороже, и точное время информативнее. Флаг `allowCountdown`
/// включает его только для Medium и крупнее, где он не отнимает у P0.
struct LessonStatusView: View {
    let status: LessonStatus
    let theme: WidgetTheme
    let palette: WidgetPalette
    var allowCountdown: Bool = true

    var body: some View {
        Text(text)
            .font(.system(size: theme.statusSize, weight: .heavy))
            .foregroundColor(palette.accent(for: status))
            .lineLimit(1)
            // Статус — текстовый, а не только цвет: в тонировании цвет
            // не несёт смысла (ТЗ п. 14, 28).
            .accessibilityLabel(accessibility)
    }

    private var text: String {
        if case .soon(let minutes) = status, allowCountdown, minutes > 0 {
            return minutes == 1 ? "ЧЕРЕЗ 1 МИН" : "ЧЕРЕЗ \(minutes) МИН"
        }
        return status.isCurrent ? "СЕЙЧАС" : "ДАЛЕЕ"
    }

    private var accessibility: String {
        switch status {
        case .current: return "Идёт сейчас"
        case .upcoming: return "Следующая пара"
        case .soon(let minutes): return "Начнётся через \(minutes) минут"
        }
    }
}

/// Заголовок дня в шапке: «СЕГОДНЯ · СРЕДА», «ЗАВТРА · ЧЕТВЕРГ», «ПТ, 23 ОКТ».
struct WidgetDayHeader: View {
    let text: String?
    let theme: WidgetTheme
    let palette: WidgetPalette
    var trailing: String? = nil

    var body: some View {
        HStack(spacing: 4) {
            if let text {
                Text(text)
                    .font(.system(size: theme.headerSize, weight: .bold))
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
            if let trailing {
                Text(trailing)
                    .font(.system(size: theme.sectionSize))
                    .foregroundColor(palette.secondary)
                    .lineLimit(1)
            }
        }
    }
}

/// Секционный заголовок: «ДАЛЕЕ СЕГОДНЯ», «ЗАВТРА · ЧЕТВЕРГ», «ЕЩЁ 2 ПАРЫ».
struct WidgetSectionHeader: View {
    let text: String
    let theme: WidgetTheme
    let palette: WidgetPalette

    var body: some View {
        Text(text)
            .font(.system(size: theme.sectionSize, weight: .heavy))
            .foregroundColor(palette.secondary)
            .lineLimit(1)
    }
}

/// Строка списка: время и аудитория в одну линию, название — во всю ширину.
///
/// Такая раскладка выбрана вместо однострочной: в одну строку названию
/// остаётся ~30 символов, и «Моделирование информационно-аналитических
/// систем» обрезается; в двух строках влезает целиком почти без прироста
/// высоты (одна строка вместо 1.4 за счёт плотного межстрочного зазора).
struct CompactLessonRow: View {
    let lesson: WLesson
    let theme: WidgetTheme
    let palette: WidgetPalette
    var status: LessonStatus? = nil
    /// Показывать ли номер пары (для дистанта и пар без аудитории).
    var showType: Bool = true

    var body: some View {
        VStack(alignment: .leading, spacing: theme.lineSpacing) {
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                LessonTimeView(
                    lesson: lesson, theme: theme, status: status,
                    palette: palette, compact: true
                )
                if showType {
                    LessonTypeBadge(lesson: lesson, theme: theme, palette: palette)
                }
                Spacer(minLength: 2)
                ClassroomView(lesson: lesson, theme: theme, compact: true)
            }
            SubjectTitleView(lesson: lesson, theme: theme, compact: true, lines: 2)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(rowAccessibility)
    }

    private var rowAccessibility: String {
        "\(lesson.start) до \(lesson.end), \(lesson.subject), аудитория \(lesson.roomDisplay)"
    }
}

/// Тонкий разделитель между смысловыми зонами и строками.
struct WidgetDivider: View {
    let palette: WidgetPalette

    var body: some View {
        Rectangle()
            .fill(palette.divider)
            .frame(height: 1)
    }
}