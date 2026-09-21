import SwiftUI
import AppMetricaCore
import Shared

@main
struct iOSApp: App {
    init() {
        KoinKt.doInitKoin()
        // Ключ — из общего Kotlin-кода (AppAnalytics): единый источник,
        // чтобы iOS и Android не разъехались по разным приложениям Metrica.
        if let configuration = AppMetricaConfiguration(apiKey: AppAnalytics.shared.apiKey) {
            AppMetrica.activate(with: configuration)
        }
        AppAnalytics.shared.setEngine(engine: AppMetricaEngine())
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
