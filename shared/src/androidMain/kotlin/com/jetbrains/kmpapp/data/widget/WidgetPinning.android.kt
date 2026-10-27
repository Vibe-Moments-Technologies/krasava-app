package com.jetbrains.kmpapp.data.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import com.jetbrains.kmpapp.data.storage.AndroidContextProvider

/**
 * Класс receiver'а живёт в модуле `androidApp`, поэтому ссылаемся по имени:
 * у shared нет и не должно быть обратной зависимости от приложения.
 */
private const val WIDGET_RECEIVER_CLASS = "com.jetbrains.kmpapp.widget.ScheduleWidgetReceiver"

actual fun isScheduleWidgetPinningSupported(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val context = AndroidContextProvider.context ?: return false
    return runCatching {
        AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported
    }.getOrDefault(false)
}

actual fun requestScheduleWidgetPin(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val context = AndroidContextProvider.context ?: return false
    return runCatching {
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return false
        val provider = ComponentName(context.packageName, WIDGET_RECEIVER_CLASS)
        manager.requestPinAppWidget(provider, null, null)
    }.getOrDefault(false)
}
