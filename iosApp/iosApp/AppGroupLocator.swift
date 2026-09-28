import Foundation

/// Определение App Group в рантайме: чем бы приложение ни переподписали
/// (GBox, esign, Sideloadly, дистрибутив Xcode), профиль лежит в бандле в
/// embedded.mobileprovision — читаем его и берём группу оттуда. Так работает
/// большинство приложений, не жёстко зафиксированных на одном профиле.
enum AppGroupLocator {
    /// Группа по умолчанию — для подписи командой проекта (App Store),
    /// где embedded.mobileprovision в бандле отсутствует.
    static let fallback = "group.ru.vibemoments.krasava"

    /// Группа из профиля, которым подписали этот бинарник. nil — профиля нет.
    static func fromProfile() -> String? {
        // Бандл приложения: в самом приложении — Bundle.main; в виджете
        // Bundle.main — это *.appex внутри PlugIns, приложение на два уровня выше.
        let appBundleURL: URL
        if Bundle.main.bundlePath.hasSuffix(".appex") {
            appBundleURL = Bundle.main.bundleURL
                .deletingLastPathComponent()  // PlugIns
                .deletingLastPathComponent()  // *.app
        } else {
            appBundleURL = Bundle.main.bundleURL
        }
        guard let profileURL = Bundle(url: appBundleURL)?
            .url(forResource: "embedded", withExtension: "mobileprovision") else { return nil }
        guard let data = try? Data(contentsOf: profileURL) else { return nil }

        // Профиль — CMS-конверт с бинарным мусором вокруг; XML-плIST внутри.
        // ISO Latin-1 декодирует любые байты без отказа.
        guard let text = String(data: data, encoding: .isoLatin1),
              let start = text.range(of: "<?xml"),
              let end = text.range(of: "</plist>", range: start.lowerBound..<text.endIndex) else { return nil }
        let xml = String(text[start.lowerBound..<end.upperBound])

        guard let plist = try? PropertyListSerialization.propertyList(
                  from: Data(xml.utf8), options: [], format: nil) as? [String: Any],
              let entitlements = plist["Entitlements"] as? [String: Any],
              let groups = entitlements["com.apple.security.application-groups"] as? [String],
              let group = groups.first(where: { !$0.isEmpty }) else { return nil }
        return group
    }

    /// Первый контейнер, который реально доступен этому процессу.
    static func availableSuiteName() -> String? {
        for group in [fromProfile(), fallback].compactMap({ $0 }) {
            if FileManager.default.containerURL(
                forSecurityApplicationGroupIdentifier: group) != nil {
                return group
            }
        }
        return nil
    }

    /// Все группы из профиля (для диагностики в меню отладки).
    static func profileGroups() -> [String] {
        let appBundleURL = Bundle.main.bundleURL
        guard let profileURL = Bundle(url: appBundleURL)?
            .url(forResource: "embedded", withExtension: "mobileprovision"),
            let data = try? Data(contentsOf: profileURL),
            let text = String(data: data, encoding: .isoLatin1),
            let start = text.range(of: "<?xml"),
            let end = text.range(of: "</plist>", range: start.lowerBound..<text.endIndex),
            let plist = try? PropertyListSerialization.propertyList(
                from: Data(String(text[start.lowerBound..<end.upperBound]).utf8),
                options: [], format: nil) as? [String: Any],
            let entitlements = plist["Entitlements"] as? [String: Any],
            let groups = entitlements["com.apple.security.application-groups"] as? [String] else {
            return []
        }
        return groups
    }

    /// Диагностика для меню отладки: что этот процесс видит.
    static func debugInfo() -> String {
        var lines: [String] = []
        lines.append("bundleId: \(Bundle.main.bundleIdentifier ?? "?")")
        let profileGroups = profileGroups()
        lines.append("групп в профиле: \(profileGroups.count)")
        for g in profileGroups {
            let alive = FileManager.default.containerURL(
                forSecurityApplicationGroupIdentifier: g) != nil
            lines.append("  \(g) — контейнер: \(alive ? "есть" : "нет")")
        }
        if profileGroups.isEmpty {
            lines.append("  (embedded.mobileprovision не найден или без групп)")
        }
        if let suite = availableSuiteName() {
            lines.append("выбранная группа: \(suite)")
        } else {
            lines.append("выбранная группа: нет доступного контейнера")
        }
        return lines.joined(separator: "\n")
    }
}
