package com.jetbrains.kmpapp

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

/**
 * Автоподстройка иконки под тему системы: днём — светлый вариант,
 * ночью — тёмный. Классическое переключение activity-alias'ов: ровно
 * один включён, состояние хранит PackageManager и живёт до след. смены.
 * Применяется при старте приложения и на каждое изменение uiMode —
 * пока приложение не запускалось после смены темы, иконка в лаунчере
 * остаётся прежней (потолок техники alias'ов).
 */
object ThemeIconSwitcher {

    // Компоненты манифеста живут в namespace (com.jetbrains.kmpapp), а НЕ в
    // applicationId (ru.l1ratch.mireaschedule): context.packageName вернул бы
    // имя несуществующего класса, переключение молча уходило в пустоту и
    // иконка пропадала (оба алиаса выключены по умолчанию).
    private const val NS = "com.jetbrains.kmpapp"

    fun apply(context: Context) {
        // Смена иконки — косметика: ни при каких обстоятельствах она не
        // должна ронять запуск приложения (был краш на части прошивок).
        try {
            val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            val pm = context.packageManager
            val light = ComponentName(NS, "$NS.AliasLight")
            val dark = ComponentName(NS, "$NS.AliasDark")
            val enable = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            val disable = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            val lightWanted = if (night) disable else enable
            val darkWanted = if (night) enable else disable
            // Не трогаем без изменений: setComponentEnabledSetting обновляет
            // иконку в лаунчере, лишние вызовы — лишние перерисовки.
            if (pm.getComponentEnabledSetting(light) != lightWanted) {
                pm.setComponentEnabledSetting(light, lightWanted, PackageManager.DONT_KILL_APP)
            }
            if (pm.getComponentEnabledSetting(dark) != darkWanted) {
                pm.setComponentEnabledSetting(dark, darkWanted, PackageManager.DONT_KILL_APP)
            }
            // MainActivity держим включённой: она цель алиасов, при её
            // выключении лаунчер потеряет точку входа.
            val main = ComponentName(NS, "$NS.MainActivity")
            if (pm.getComponentEnabledSetting(main) != enable) {
                pm.setComponentEnabledSetting(main, enable, PackageManager.DONT_KILL_APP)
            }
        } catch (e: Throwable) {
            println("ThemeIconSwitcher: не удалось переключить иконку: ${e.message}")
        }
    }
}
