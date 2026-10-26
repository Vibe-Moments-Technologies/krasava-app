package com.jetbrains.kmpapp.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.jetbrains.kmpapp.data.widget.WidgetBridge

/**
 * Мост между приложением и виджетом на Android.
 *
 * iOS пишет снапшот в App Group (`WidgetSync.push()` в `iOSApp.swift`).
 * Android-аналог общего контейнера — приватный `SharedPreferences`-файл
 * приложения: его читает и приложение, и процесс виджета.
 */
object ScheduleWidgetStore {
    private const val PREFS = "krasava_widget"
    private const val KEY_PAYLOAD = "widget_payload"

    fun read(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PAYLOAD, null)

    /**
     * Забирает свежий снапшот из общего `WidgetBridge` и, если он изменился,
     * сохраняет его и обновляет все экземпляры виджета.
     *
     * Вызывается из `MainActivity` при появлении/уходе приложения в фон —
     * так же, как iOS вызывает `WidgetSync.push()` по `scenePhase`.
     */
    suspend fun publish(context: Context) {
        val appContext = context.applicationContext
        val json = runCatching { WidgetBridge.payloadJson() }.getOrNull() ?: return
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_PAYLOAD, null) == json) return
        prefs.edit().putString(KEY_PAYLOAD, json).commit()
        runCatching { ScheduleWidget().updateAll(appContext) }
    }
}
