package com.jetbrains.kmpapp.appicon

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.jetbrains.kmpapp.data.appicon.AppIconManager

class AndroidAppIconEngine(private val context: Context) : AppIconManager.IconEngine {
    private val pm: PackageManager get() = context.packageManager

    // applicationId (ru.vibemoments.krasava) отличается от namespace/классов,
    // поэтому имя компонента задаём как в манифесте, а не через packageName.
    private val aliases = listOf(
        ComponentName(context, "com.jetbrains.kmpapp.DefaultLightAlias"),
        ComponentName(context, "com.jetbrains.kmpapp.DefaultDarkAlias"),
        ComponentName(context, "com.jetbrains.kmpapp.GriffonLightAlias"),
        ComponentName(context, "com.jetbrains.kmpapp.GriffonDarkAlias")
    )

    override fun applyIcon(name: String, dark: Boolean) {
        val target = when {
            name == AppIconManager.ICON_ALT && dark -> "GriffonDarkAlias"
            name == AppIconManager.ICON_ALT -> "GriffonLightAlias"
            dark -> "DefaultDarkAlias"
            else -> "DefaultLightAlias"
        }
        aliases.forEach { alias ->
            val enable = alias.className.endsWith(target)
            setEnabled(alias, enable)
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
}
