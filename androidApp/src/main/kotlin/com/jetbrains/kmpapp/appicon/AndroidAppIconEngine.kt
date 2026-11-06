package com.jetbrains.kmpapp.appicon

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.jetbrains.kmpapp.data.appicon.AppIconManager

class AndroidAppIconEngine(private val context: Context) : AppIconManager.IconEngine {
    private val pm: PackageManager get() = context.packageManager

    // applicationId (ru.vibemoments.krasava) отличается от namespace/классов,
    // поэтому имя компонента задаём как в манифесте, а не через packageName.
    private val defaultAlias = ComponentName(context, "com.jetbrains.kmpapp.DefaultIconAlias")
    private val altAlias = ComponentName(context, "com.jetbrains.kmpapp.AltIconAlias")

    override fun applyIcon(name: String) {
        val (enable, disable) = when (name) {
            AppIconManager.ICON_ALT -> altAlias to defaultAlias
            else -> defaultAlias to altAlias
        }
        setEnabled(enable, true)
        setEnabled(disable, false)
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
}
