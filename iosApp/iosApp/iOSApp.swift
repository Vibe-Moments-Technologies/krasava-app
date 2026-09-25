import SwiftUI
import Shared

@main
struct iOSApp: App {
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
        }
    }
}
