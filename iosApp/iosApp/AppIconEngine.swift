import UIKit
import UserNotifications
import CFNetwork
import Shared

// Оба платформенных движка живут в одном файле: один Swift-файл в фазе
// Sources (NotificationsEngine.swift) детерминированно выпадал из плана
// сборки Xcode при корректном pbxproj — здесь компиляция гарантирована.
final class AppIconEngine: AppIconManagerIconEngine {
    func applyIcon(name: String) {
        // "default" -> nil = вернуть первичную иконку из asset catalog.
        // Литерал, а не AppIconManager.ICON_DEFAULT: const val не экспортируется
        // в Swift (inline на стороне Kotlin), значение зафиксировано в common-коде.
        let iconName: String? = name == "default" ? nil : name
        // Ошибки (например, PNG с альфа-каналом iOS отвергает) логируем,
        // чтобы сбой смены иконки не был немым.
        UIApplication.shared.setAlternateIconName(iconName) { error in
            if let error = error {
                print("AppIconEngine: setAlternateIconName failed: \(error.localizedDescription)")
            }
        }
    }
}

/// iOS-движок локальных напоминаний о занятиях (UNUserNotificationCenter).
/// Разрешение запрашивается только в момент включения тумблера в настройках.
final class NotificationsEngine: NotificationsManagerNotificationEngine {
    func requestAuthorization() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
    }

    func schedule(id: String, title: String, body: String, dateEpochMillis: Int64) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        // Тестовые уведомления: огонь по секундам, календарь округляет
        // до минут (dateComponents без секунд) и убивал бы их пачками.
        let interval = TimeInterval(dateEpochMillis) / 1000 - Date().timeIntervalSince1970
        let trigger: UNNotificationTrigger
        if interval <= 90 {
            trigger = UNTimeIntervalNotificationTrigger(timeInterval: max(1, interval), repeats: false)
        } else {
            let date = Date(timeIntervalSince1970: TimeInterval(dateEpochMillis) / 1000)
            let components = Calendar.current.dateComponents(
                [.year, .month, .day, .hour, .minute], from: date)
            trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
        }
        UNUserNotificationCenter.current().add(
            UNNotificationRequest(identifier: id, content: content, trigger: trigger))
    }

    func cancelAll() {
        // Снимаем только партию занятий (идентификаторы lesson-…):
        // removeAllPendingNotificationRequests сносил бы и тестовые
        // уведомления, поставленные из отладки.
        let center = UNUserNotificationCenter.current()
        center.getPendingNotificationRequests { requests in
            let lessonIds = requests.map(\.identifier).filter { $0.hasPrefix("lesson-") }
            center.removePendingNotificationRequests(withIdentifiers: lessonIds)
        }
    }
}

/// Показ уведомлений, когда приложение открыто: без делегата iOS молча
/// гасит баннер в форграунде (тест из отладки «не приходил», хотя
/// напоминания о парах в фоне доставлялись).
final class NotificationPresenter: NSObject, UNUserNotificationCenterDelegate {
    static let shared = NotificationPresenter()

    func attach() {
        UNUserNotificationCenter.current().delegate = self
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound])
    }
}

/// iOS-детектор VPN: публичного API «VPN включён» нет, используем
/// канонический «AppsFlyer-style» разбор системных настроек прокси —
/// ключ __SCOPED__ содержит интерфейсы активных туннелей
/// (WireGuard, OpenVPN, корпоративные NEPacketTunnelProvider).
final class VpnEngine: VpnStatusEngine {
    func isVpnActive() -> Bool {
        guard let settings = CFNetworkCopySystemProxySettings()?.takeRetainedValue() as? [String: Any],
              let scoped = settings["__SCOPED__"] as? [String: Any] else { return false }
        for key in scoped.keys {
            let k = key.lowercased()
            if k.contains("tap") || k.contains("tun") || k.contains("ppp")
                || k.contains("ipsec") || k.contains("utun") {
                return true
            }
        }
        return false
    }
}
