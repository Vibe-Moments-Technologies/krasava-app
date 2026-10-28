package com.jetbrains.kmpapp.data.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import com.jetbrains.kmpapp.data.storage.AndroidContextProvider

/**
 * Классы receiver'а и конфигуратора живут в модуле `androidApp`, поэтому
 * ссылаемся по имени: у shared нет и не должно быть обратной зависимости
 * от приложения.
 */
private const val WIDGET_RECEIVER_CLASS = "com.jetbrains.kmpapp.widget.ScheduleWidgetReceiver"
private const val WIDGET_CONFIGURE_CLASS = "com.jetbrains.kmpapp.widget.ScheduleWidgetConfigureActivity"

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
        // Success-callback: без него многие лаунчеры при добавлении ИЗ
        // ПРИЛОЖЕНИЯ пропускают конфигуратор (даже если он объявлен в
        // appwidget-provider). Колбэк срабатывает после подтверждения
        // пользователем — и открывает конфигуратор гарантированно, на
        // любом лаунчере, с уже валидным id виджета.
        val configIntent = Intent().apply {
            setClassName(context.packageName, WIDGET_CONFIGURE_CLASS)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val configPendingIntent = PendingIntent.getActivity(
            context,
            0,
            configIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        manager.requestPinAppWidget(provider, null, configPendingIntent)
    }.getOrDefault(false)
}
