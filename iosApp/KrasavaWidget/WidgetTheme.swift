import SwiftUI
import WidgetKit

// MARK: - Дизайн-система виджетов
//
// Единые токены типографики, интервалов и цветов для всех четырёх размеров.
// Требование ТЗ (п. 13): не привязывать интерфейс к произвольным пиксельным
// константам, но и не разбрасывать размеры шрифтов по вью. Всё, что относится
// к оформлению, живёт здесь; вью только выбирают токен.

/// Плотность компоновки. Режим подбирается по содержимому, а не настройкой
/// пользователя (ТЗ п. 25): при длинных названиях виджет переходит на более
/// компактные строки, чтобы не обрезать аудиторию и время.
enum WidgetDensity {
    case comfortable
    case compact
    case dense

    /// Порог длины названия дисциплины, после которого нужен более плотный
    /// режим: «Моделирование информационно-аналитических систем» — 46 символов.
    static func forSubject(_ subject: String, in availableHeight: CGFloat) -> WidgetDensity {
        let long = subject.count > 46
        if availableHeight < 300 || (long && availableHeight < 360) {
            return .dense
        }
        if long || availableHeight < 420 {
            return .compact
        }
        return .comfortable
    }
}

/// Типографика и интервалы виджета.
///
/// Иерархия по ТЗ (п. 13): аудитория — самый заметный элемент, время — второй
/// акцент, название — основной текст с максимальной полнотой, статус —
/// небольшой акцентный заголовок.
struct WidgetTheme {
    let density: WidgetDensity
    /// Доступная высота виджета в точках — влияет на масштаб аудитории.
    let availableHeight: CGFloat

    init(density: WidgetDensity = .comfortable, availableHeight: CGFloat = 400) {
        self.density = density
        self.availableHeight = availableHeight
    }

    // MARK: Интервалы

    /// Внешний отступ контента от края виджета.
    var padding: CGFloat {
        switch density {
        case .comfortable: return 14
        case .compact: return 12
        case .dense: return 10
        }
    }

    /// Зазор между смысловыми блоками (шапка, пары).
    var blockSpacing: CGFloat {
        switch density {
        case .comfortable: return 8
        case .compact: return 6
        case .dense: return 5
        }
    }

    /// Зазор между строками внутри одной пары (служебная строка / название).
    var lineSpacing: CGFloat {
        switch density {
        case .comfortable: return 2
        case .compact: return 1.5
        case .dense: return 1
        }
    }

    /// Зазор между отдельными парами в списке.
    var rowSpacing: CGFloat {
        switch density {
        case .comfortable: return 10
        case .compact: return 8
        case .dense: return 6
        }
    }

    // MARK: Типографика

    /// Номер аудитории — главный элемент (P0). Крупнее всего остального.
    var roomSize: CGFloat {
        switch density {
        case .comfortable: return availableHeight > 600 ? 44 : 30
        case .compact: return availableHeight > 600 ? 38 : 26
        case .dense: return availableHeight > 600 ? 32 : 22
        }
    }

    /// Корпус («С-20») — рядом с номером, заметно мельче.
    var buildingSize: CGFloat {
        switch density {
        case .comfortable: return 15
        case .compact: return 13
        case .dense: return 11
        }
    }

    /// Время занятия — второй акцент (P0). Всегда одной строкой.
    var timeSize: CGFloat {
        switch density {
        case .comfortable: return 15
        case .compact: return 13
        case .dense: return 12
        }
    }

    /// Шрифт времени в компактной строке списка (Medium/Large/Extra).
    var rowTimeSize: CGFloat {
        switch density {
        case .comfortable: return 13
        case .compact: return 12
        case .dense: return 11
        }
    }

    /// Название дисциплины — основной текст (P1).
    var subjectSize: CGFloat {
        switch density {
        case .comfortable: return 15
        case .compact: return 13
        case .dense: return 12
        }
    }

    /// Название в компактной строке списка.
    var rowSubjectSize: CGFloat {
        switch density {
        case .comfortable: return 13
        case .compact: return 12
        case .dense: return 11
        }
    }

    /// Статус («СЕЙЧАС», «ДАЛЕЕ») — небольшой акцентный заголовок.
    var statusSize: CGFloat { 11 }

    /// Заголовок дня («СЕГОДНЯ · СРЕДА») в шапке.
    var headerSize: CGFloat {
        switch density {
        case .comfortable: return 13
        case .compact: return 12
        case .dense: return 11
        }
    }

    /// Секционный заголовок («ДАЛЕЕ СЕГОДНЯ», «ЗАВТРА · ЧЕТВЕРГ»).
    var sectionSize: CGFloat { 11 }

    /// Бейдж типа занятия (P3).
    var badgeSize: CGFloat {
        switch density {
        case .comfortable: return 10
        case .compact: return 9.5
        case .dense: return 9
        }
    }

    /// Подпись аудитории в компактной строке.
    var rowRoomSize: CGFloat {
        switch density {
        case .comfortable: return 13
        case .compact: return 12
        case .dense: return 11
        }
    }
}

// MARK: - Цвета

/// Режим рендеринга виджета в терминах, не зависящих от версии iOS.
///
/// `WidgetRenderingMode` доступен только с iOS 16, а минимальная версия
/// проекта — 15.3, поэтому собственный тип: значение подставляется там, где
/// системный API доступен, иначе остаётся `.fullColor`.
enum WidgetPaletteMode {
    case fullColor
    case tinted
}

/// Палитра виджета.
///
/// Отдельный тип, а не набор `.green`/`.secondary` по месту: цвета должны
/// поддерживать режимы тонирования (ТЗ п. 14), когда система переопределяет
/// фон и может убрать цветность. В режиме тонирования акценты становятся
/// монохромными, а смысл переносится на текст — поэтому вью дополнительно
/// показывает словесный статус, а не полагается только на цвет.
struct WidgetPalette {
    let mode: WidgetPaletteMode

    init(mode: WidgetPaletteMode = .fullColor) {
        self.mode = mode
    }

    /// В тонированном режиме цвет как указатель смысла ненадёжен.
    private var isTinted: Bool {
        if case .tinted = mode { return true }
        return false
    }

    var primary: Color { .primary }
    var secondary: Color { .secondary }
    var muted: Color { isTinted ? .secondary : Color.gray }

    var divider: Color { Color.secondary.opacity(isTinted ? 0.25 : 0.18) }

    /// Акцент идущей пары.
    var currentAccent: Color { isTinted ? .primary : .green }
    /// Акцент будущей пары.
    var upcomingAccent: Color { isTinted ? .primary : .blue }

    /// Цвет бейджа типа занятия.
    func typeColor(_ type: String) -> Color {
        guard !isTinted else { return .primary }
        switch type {
        case "ЛК": return .blue
        case "ПР": return .green
        case "ЛАБ": return .orange
        case "ДОП": return .pink
        case "СРС": return .purple
        default: return .gray
        }
    }

    /// Цвет текста статуса.
    func accent(for status: LessonStatus) -> Color {
        status.isCurrent ? currentAccent : upcomingAccent
    }
}