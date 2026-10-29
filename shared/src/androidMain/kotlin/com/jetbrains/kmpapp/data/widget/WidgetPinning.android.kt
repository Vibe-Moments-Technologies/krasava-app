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
private const val WIDGET_APPLY_CLASS = "com.jetbrains.kmpapp.widget.ScheduleWidgetApplyActivity"

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
        // Success-callback: срабатывает после подтверждения пользователем.
        // Из приложения параметры выбираются ЗАРАНЕЕ — внутренний
        // конфигуратор (решение владельца: лаунчеры системный
        // конфигуратор при добавлении из приложения всё равно не
        // показывают). Колбэк ведёт в неинтерактивную
        // ScheduleWidgetApplyActivity: она пишет выбор в Glance-состояние
        // нового экземпляра (нет параметров — просто закрывается) и
        // помечает «диалог не был подавлен» для экрана настроек.
        val configIntent = Intent().apply {
            setClassName(context.packageName, WIDGET_APPLY_CLASS)
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

