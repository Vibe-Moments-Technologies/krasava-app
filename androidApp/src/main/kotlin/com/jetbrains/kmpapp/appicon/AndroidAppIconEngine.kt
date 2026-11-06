package com.jetbrains.kmpapp.appicon

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.jetbrains.kmpapp.data.appicon.AppIconManager

/**
 * Android умеет менять иконку только включением/выключением activity-alias.
 * Это может закрыть задачу на некоторых лаунчерах, поэтому:
 *  - выбор иконки/темы сразу сохраняется как «желаемый»;
 *  - alias применяется, когда приложение уходит в фон (MainActivity.onStop);
 *  - если процесс убили раньше, недоприменённое состояние остаётся в prefs
 *    и применяется при следующем запуске;
 *  - «применено» фиксируется только после проверки фактического состояния alias.
 */
class AndroidAppIconEngine(context: Context) : AppIconManager.IconEngine {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val pm: PackageManager get() = appContext.packageManager

    private val aliases = linkedMapOf(
        KEY_DEFAULT_LIGHT to ComponentName(appContext, "com.jetbrains.kmpapp.DefaultLightAlias"),
        KEY_DEFAULT_DARK to ComponentName(appContext, "com.jetbrains.kmpapp.DefaultDarkAlias"),
        KEY_GRIFFON_LIGHT to ComponentName(appContext, "com.jetbrains.kmpapp.GriffonLightAlias"),
        KEY_GRIFFON_DARK to ComponentName(appContext, "com.jetbrains.kmpapp.GriffonDarkAlias")
    )

    /** Только запоминаем желаемое; реальное переключение — в [applyPending]. */
    override fun applyIcon(name: String, dark: Boolean) {
        prefs.edit()
            .putString(KEY_WANTED_NAME, name)
            .putBoolean(KEY_WANTED_DARK, dark)
            .apply()
    }

    fun hasPending(): Boolean {
        val target = wantedKey() ?: return false
        return prefs.getString(KEY_APPLIED, null) != target || !isApplied(target)
    }

    /** Применяет желаемую иконку. true — на рабочем столе уже нужный alias. */
    fun applyPending(): Boolean {
        val target = wantedKey() ?: return true
        if (prefs.getString(KEY_APPLIED, null) == target && isApplied(target)) return true

        // Сначала включаем нужный alias, затем выключаем остальные.
        setEnabled(aliases.getValue(target), true)
        aliases.filterKeys { it != target }.forEach { (_, alias) ->
            setEnabled(alias, false)
        }

        if (!isApplied(target)) return false
        prefs.edit().putString(KEY_APPLIED, target).apply()
        return true
    }

    private fun wantedKey(): String? {
        val name = prefs.getString(KEY_WANTED_NAME, null) ?: return null
        val dark = prefs.getBoolean(KEY_WANTED_DARK, false)
        return if (name == AppIconManager.ICON_ALT) {
            if (dark) KEY_GRIFFON_DARK else KEY_GRIFFON_LIGHT
        } else {
            if (dark) KEY_DEFAULT_DARK else KEY_DEFAULT_LIGHT
        }
    }

    private fun isApplied(target: String): Boolean =
        aliases.all { (key, alias) ->
            val state = runCatching { pm.getComponentEnabledSetting(alias) }
                .getOrDefault(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
            if (key == target) {
                state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            } else {
                state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
        }

    private fun setEnabled(component: ComponentName, enabled: Boolean) {
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        runCatching {
            pm.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
        }
    }

    private companion object {
        const val PREFS = "krasava_app_icon"
        const val KEY_WANTED_NAME = "wanted_name"
        const val KEY_WANTED_DARK = "wanted_dark"
        const val KEY_APPLIED = "applied"
        const val KEY_DEFAULT_LIGHT = "default_light"
        const val KEY_DEFAULT_DARK = "default_dark"
        const val KEY_GRIFFON_LIGHT = "griffon_light"
        const val KEY_GRIFFON_DARK = "griffon_dark"
    }
}
