import SwiftUI
import Shared
import WidgetKit

/// Запись снапшота расписания в App Group для виджета.
/// Нет контейнера (сборка/подпись без App Group) — молча пропускаем:
/// UserDefaults(suiteName:) без entitlements не падает, а пишет в изолированную
/// песочницу — проверяем контейнер явно, чтобы не врать, что синхронизировали.
enum WidgetSync {
    // Группа из mobileprovision, которым переподписывается IPA: профиль
    // разрешает только group.c2fefdd0db2e255e.* — своя группа невозможна.
    // Совпадает с WidgetData.suiteName в KrasavaWidget.swift.
    static let suiteName = "group.c2fefdd0db2e255e.1"
    static let key = "widget_snapshot"

    static func push() {
        guard FileManager.default.containerURL(
            forSecurityApplicationGroupIdentifier: suiteName) != nil,
            let json = WidgetBridge.shared.snapshotJson(),
            let defaults = UserDefaults(suiteName: suiteName) else { return }
        defaults.set(json, forKey: key)
        WidgetCenter.shared.reloadAllTimelines()
    }
}

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase

    init() {
        KoinKt.doInitKoin()
        // Диагностика (Sentry) поднимется сама, если включён тумблер в отладке.
        AppDiagnostics.shared.setEngine(engine: SentryDiagnosticsEngine())
        AppIconManager.shared.setEngine(newEngine: AppIconEngine())
        // Держим ссылку: движок нужен и после регистрации (уборка с прошлого
        // запуска) — до первого планирования, чтобы не гонять с ним гонку.
        let notifications = NotificationsEngine()
        NotificationsManager.shared.setEngine(newEngine: notifications)
        notifications.sweepStaleLessonReminders()
        NotificationPresenter.shared.attach()
        VpnStatus.shared.setEngine(newEngine: VpnEngine())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onAppear { WidgetSync.push() }
        }
        .onChange(of: scenePhase) { phase in
            // Перед уходом в фон и по возврате — виджет всегда со свежим снимком.
            if phase == .background || phase == .active {
                WidgetSync.push()
            }
        }
    }
}
