import AppIntents
import WidgetKit

// MARK: - Конфигурация виджета
//
// По решению владельца параметр один: группа. Виджет работает по умолчанию
// (берёт расписание, выбранное для уведомлений), но конкретному виджету можно
// назначить другое расписание через «Изменить виджет».

/// Расписание как вариант выбора в «Изменить виджет» → «Группа».
/// Требует iOS 17: AppIntent-конфигурация в WidgetKit появилась там же.
@available(iOS 17.0, *)
struct WidgetTargetEntity: AppEntity, Identifiable {
    let id: Int
    let title: String
    let typeName: String

    static var typeDisplayRepresentation: TypeDisplayRepresentation {
        TypeDisplayRepresentation(name: "Расписание")
    }

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(
            // Названия групп и типов приходят из данных приложения, а не из
            // каталога локализации — подставляем как есть, без строковых ключей.
            title: LocalizedStringResource(stringLiteral: title),
            subtitle: LocalizedStringResource(stringLiteral: typeName)
        )
    }

    static var defaultQuery = WidgetTargetQuery()

    init(id: Int, title: String, typeName: String) {
        self.id = id
        self.title = title
        self.typeName = typeName
    }

    init(_ target: WTarget) {
        self.init(id: target.id, title: target.title, typeName: target.typeName)
    }
}

/// Источник вариантов для списка выбора. Читает тот же общий контейнер,
/// что и виджет, — сеть и Koin не нужны.
@available(iOS 17.0, *)
struct WidgetTargetQuery: EntityQuery {
    func entities(for identifiers: [Int]) async throws -> [WidgetTargetEntity] {
        let all = WidgetData.availableTargets().map(WidgetTargetEntity.init)
        // Возвращаем и запрошенные, и просто все доступные: если группа была
        // удалена в приложении, виджет не должен остаться без вариантов.
        let requested = all.filter { identifiers.contains($0.id) }
        return requested.isEmpty ? all : requested
    }

    func suggestedEntities() async throws -> [WidgetTargetEntity] {
        WidgetData.availableTargets().map(WidgetTargetEntity.init)
    }

    func defaultResult() async -> WidgetTargetEntity? {
        // nil = «не выбран», и это не ошибка: виджет работает по умолчанию,
        // подставляя расписание уведомлений (решение владельца). Пользователь
        // может переопределить его для конкретного виджета.
        nil
    }
}

/// Параметры виджета: пока только группа.
@available(iOS 17.0, *)
struct KrasavaWidgetIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource { "Расписание" }
    static var description: IntentDescription {
        IntentDescription("Выберите группу, преподавателя или аудиторию для виджета.")
    }

    @Parameter(title: "Группа")
    var target: WidgetTargetEntity?
}

// MARK: - Кнопка «Обновить»

/// Пересчёт виджета по нажатию кнопки внутри него.
///
/// Что реально делает: перечитывает снапшот из общего контейнера и строит
/// новый Timeline от текущего момента. Поэтому статус («СЕЙЧАС»/«ДАЛЕЕ») и
/// полоса прогресса становятся актуальными без запуска приложения — именно
/// этого просил владелец, заметив, что прогресс обновлялся только при
/// открытии приложения.
///
/// Чего не делает: не ходит в сеть за новым расписанием. Расширение виджета
/// сознательно не тянет данные из интернета (память, время, батарея);
/// расписание обновляет приложение, а кнопка пересчитывает то, что уже
/// сохранено. Это честно отражено в названии и описании.
@available(iOS 17.0, *)
struct RefreshWidgetIntent: AppIntent {
    static var title: LocalizedStringResource = "Обновить виджет"
    static var description: IntentDescription = IntentDescription(
        "Пересчитать виджет из сохранённого расписания: статус и прогресс пары станут актуальными."
    )

    /// Не показывать в приложении «Команды»: это служебное действие виджета.
    static var isDiscoverable: Bool = false

    func perform() async throws -> some IntentResult {
        WidgetCenter.shared.reloadTimelines(ofKind: "KrasavaWidget")
        return .result()
    }
}