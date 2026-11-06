package com.jetbrains.kmpapp.data.theme

import android.content.res.Configuration
import com.jetbrains.kmpapp.data.storage.AndroidContextProvider

actual fun isSystemDarkTheme(): Boolean {
    val ctx = AndroidContextProvider.context ?: return false
    return (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
}
