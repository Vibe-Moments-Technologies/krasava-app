import AppIntents
import WidgetKit

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
            // Данные пользователя, не локализуемые строки — литеральные.
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
        // Намеренно nil: виджет не должен самовольно выбирать группу,
        // пользователь задаёт её через «Изменить виджет».
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