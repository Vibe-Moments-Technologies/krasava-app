import SwiftUI

// MARK: - Дизайн-система виджетов
//
// Токены из согласованных макетов: графитовая поверхность, светлый текст,
// голубой акцент статуса, зелёный акцент практики. Значения — стартовая
// спецификация (ТЗ п. 2.1), а не жёстко зашитый тёмный фон: светлая тема имеет
// собственные семантические токены, а системное тонирование учитывается отдельно.
//
// Статус пары и контекст даты живут в WidgetKitLogic (WidgetModels.swift):
// по ТЗ п. 8.3 их формирует бизнес-логика, а не View.

// MARK: - Сетка отступов (ТЗ п. 2.3)

/// Шаг сетки. Используется вместо случайных отступов по месту.
enum Space {
    static let s2: CGFloat = 2
    static let s4: CGFloat = 4
    static let s6: CGFloat = 6
    static let s8: CGFloat = 8
    static let s10: CGFloat = 10
    static let s12: CGFloat = 12
    static let s16: CGFloat = 16
}

// MARK: - Плотность (ТЗ п. 9.2, 9.3)

/// Уровень плотности компоновки.
///
/// Regular — нормальные размеры и полные метаданные, Compact — меньше
/// промежутков и вторичных данных, Minimal — только обязательные сведения.
/// Выбирается по фактическому размеру контейнера, а не по семейству:
/// размеры виджетов отличаются между устройствами (ТЗ п. 9.1).
enum WidgetDensity: Int, Comparable {
    case minimal = 0
    case compact = 1
    case regular = 2

    static func < (a: WidgetDensity, b: WidgetDensity) -> Bool {
        a.rawValue < b.rawValue
    }

    /// Порог по высоте контейнера в точках.
    static func forHeight(_ height: CGFloat) -> WidgetDensity {
        if height < 150 { return .minimal }
        if height < 320 { return .compact }
        return .regular
    }

    /// Показывать ли преподавателя — вторичные метаданные.
    var showsTeacher: Bool { self >= .regular }

    /// Показывать ли индикатор нескольких групп.
    var showsGroupIndicator: Bool { self >= .compact }

    /// Максимум строк названия главной дисциплины.
    var subjectLines: Int {
        switch self {
        case .minimal: return 2
        case .compact: return 2
        case .regular: return 3
        }
    }

    /// Максимум строк названия следующей пары.
    var rowSubjectLines: Int {
        self >= .regular ? 2 : 1
    }

    /// Зазор между смысловыми блоками.
    var blockSpacing: CGFloat {
        switch self {
        case .minimal: return Space.s4
        case .compact: return Space.s6
        case .regular: return Space.s8
        }
    }

    /// Зазор между строками внутри одного блока.
    var lineSpacing: CGFloat {
        switch self {
        case .minimal, .compact: return Space.s2
        case .regular: return Space.s4
        }
    }

    /// Зазор между отдельными парами в списке.
    var rowSpacing: CGFloat {
        switch self {
        case .minimal: return Space.s6
        case .compact: return Space.s8
        case .regular: return Space.s10
        }
    }

    /// Высота одной строки следующей пары: по ней считается, сколько влезает.
    var rowHeight: CGFloat {
        switch self {
        case .minimal: return 34
        case .compact: return 42
        case .regular: return 48
        }
    }
}

// MARK: - Типографика (ТЗ п. 2.2)

/// Набор размеров шрифта для конкретного размера виджета.
///
/// Диапазоны из ТЗ: Small 20–26 pt аудитория, Medium 16–20, Large 24–30,
/// Extra Large 27–34. Внутри диапазона размер выбирается по плотности.
struct WidgetTypography {
    let density: WidgetDensity

    private func pick(_ regular: CGFloat, _ compact: CGFloat, _ minimal: CGFloat) -> CGFloat {
        switch density {
        case .regular: return regular
        case .compact: return compact
        case .minimal: return minimal
        }
    }

    /// Аудитория главной пары. `range` — диапазон из таблицы ТЗ.
    func room(_ range: ClosedRange<CGFloat>) -> CGFloat {
        pick(range.upperBound, (range.lowerBound + range.upperBound) / 2, range.lowerBound)
    }

    /// Время главной пары.
    func primaryTime(_ range: ClosedRange<CGFloat>) -> CGFloat {
        pick(range.upperBound, (range.lowerBound + range.upperBound) / 2, range.lowerBound)
    }

    /// Название главной дисциплины.
    func subject(_ range: ClosedRange<CGFloat>) -> CGFloat {
        pick(range.upperBound, (range.lowerBound + range.upperBound) / 2, range.lowerBound)
    }

    /// Статус.
    func status(_ range: ClosedRange<CGFloat>) -> CGFloat {
        pick(range.upperBound, range.upperBound, range.lowerBound)
    }

    /// Аудитория следующей пары.
    func rowRoom(_ range: ClosedRange<CGFloat>) -> CGFloat {
        pick(range.upperBound, (range.lowerBound + range.upperBound) / 2, range.lowerBound)
    }

    /// Время следующей пары.
    func rowTime(_ range: ClosedRange<CGFloat>) -> CGFloat {
        pick(range.upperBound, (range.lowerBound + range.upperBound) / 2, range.lowerBound)
    }

    /// Название следующей пары.
    func rowSubject(_ range: ClosedRange<CGFloat>) -> CGFloat {
        pick(range.upperBound, (range.lowerBound + range.upperBound) / 2, range.lowerBound)
    }

    /// Преподаватель и корпусные метаданные.
    var teacher: CGFloat { pick(12, 11, 10) }

    /// Служебные подписи: заголовки дней, «ДАЛЕЕ».
    var caption: CGFloat { pick(12, 11, 10) }

    /// Бейдж типа занятия.
    var badge: CGFloat { pick(11, 10, 9) }
}

// MARK: - Палитра (ТЗ п. 2.1, 15.4)

/// Семантические цвета.
///
/// Тёмная схема использует графитовые значения из макета. Светлая — не
/// «инверсия тёмной»: подобраны собственные значения с достаточным контрастом
/// на светлом фоне (ТЗ п. 15.4).
struct WidgetPalette {
    let colorScheme: ColorScheme
    /// Режим тонирования: акценты становятся монохромными.
    let isTinted: Bool

    init(colorScheme: ColorScheme = .dark, isTinted: Bool = false) {
        self.colorScheme = colorScheme
        self.isTinted = isTinted
    }

    private var isDark: Bool { colorScheme == .dark }

    // MARK: Поверхности

    /// Общий фон виджета.
    var surfaceBackground: Color {
        isDark ? Color(hex: 0x292C31) : Color(hex: 0xF2F3F5)
    }

    /// Дополнительная поверхность: выделение главной пары в Extra Large.
    var subtleSurface: Color {
        isDark ? Color(hex: 0x353940) : Color(hex: 0xE6E8EC)
    }

    /// Разделители.
    var divider: Color {
        isDark ? Color(hex: 0x51545D) : Color(hex: 0xC7CAD1)
    }

    // MARK: Текст

    /// Аудитория и время главной пары.
    var primaryText: Color {
        isDark ? Color(hex: 0xF5F5F7) : Color(hex: 0x1C1D21)
    }

    /// Названия дисциплин.
    var subjectText: Color {
        isDark ? Color(hex: 0xE5E7EC) : Color(hex: 0x2B2D33)
    }

    /// Преподаватель, корпусные метаданные.
    var secondaryText: Color {
        isDark ? Color(hex: 0xB6BAC4) : Color(hex: 0x5C606B)
    }

    /// Служебные подписи: заголовки дней, «ДАЛЕЕ».
    var mutedText: Color {
        isDark ? Color(hex: 0x969BA6) : Color(hex: 0x777C88)
    }

    // MARK: Акценты

    /// «СЕЙЧАС», «ДАЛЕЕ». В тонировании цвет смысла не несёт — статус
    /// дополнительно подписан текстом (ТЗ п. 15.3).
    var activeAccent: Color {
        if isTinted { return primaryText }
        return isDark ? Color(hex: 0x65B5FF) : Color(hex: 0x0071C5)
    }

    /// Практика и другие типы занятий.
    var practiceAccent: Color {
        if isTinted { return primaryText }
        return isDark ? Color(hex: 0x78D99D) : Color(hex: 0x1B7F45)
    }

    // MARK: Тип занятия

    /// Цвет бейджа типа. Мягкое выделение, неизвестные типы — нейтральные.
    func typeColor(_ type: String) -> Color {
        guard !isTinted else { return primaryText }
        switch type {
        case "ПР":
            return practiceAccent
        case "ЛК":
            return isDark ? Color(hex: 0x8FB8FF) : Color(hex: 0x2F5FBF)
        case "ЛАБ", "ЛБ":
            return isDark ? Color(hex: 0xE3B368) : Color(hex: 0x8A5A00)
        case "ЭКЗ", "ЗАЧ", "КР":
            return isDark ? Color(hex: 0xE58C8C) : Color(hex: 0xA02B2B)
        default:
            return secondaryText
        }
    }

    /// Фон бейджа: мягкая заливка того же тона.
    func typeBackground(_ type: String) -> Color {
        typeColor(type).opacity(isTinted ? 0 : 0.16)
    }
}

// MARK: - Хелпер цвета

extension Color {
    /// Цвет из 0xRRGGBB — палитра задаётся в одном месте и совпадает
    /// с таблицей токенов из макета.
    init(hex: UInt32) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: 1
        )
    }
}
