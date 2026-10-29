package com.jetbrains.kmpapp.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.jetbrains.kmpapp.data.widget.WidgetPinFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Применение параметров, выбранных во внутреннем конфигураторе приложения
 * (см. SettingsScreen): лаунчеры не показывают системный конфигуратор при
 * добавлении виджета из приложения, поэтому выбор делается ДО закрепления,
 * а эта активность стартует success-callback'ом после подтверждения —
 * пишет выбор в Glance-состояние нового экземпляра и обновляет виджет.
 *
 * Не интерактивна: мелькает на долю секунды и закрывается — параметры
 * уже выбраны, переспрашивать нечего.
 */
class ScheduleWidgetApplyActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Метка для экрана настроек: диалог закрепления не был подавлен,
        // и закрепление подтверждено (эта активность стартует только из
        // success-callback).
        WidgetPinFeedback.lastConfigureOpenedAtMillis = System.currentTimeMillis()

        val config = WidgetPinFeedback.pendingConfig
        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (config == null || appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        // Одноразовые: повторное срабатывание колбэка не должно
        // перезаписывать настройки вручную добавленных виджетов.
        WidgetPinFeedback.pendingConfig = null

        CoroutineScope(Dispatchers.IO).launch {
            val manager = GlanceAppWidgetManager(this@ScheduleWidgetApplyActivity)
            val glanceId = runCatching { manager.getGlanceIdBy(appWidgetId) }.getOrNull()
            if (glanceId != null) {
                runCatching {
                    updateAppWidgetState(
                        this@ScheduleWidgetApplyActivity,
                        glanceId
                    ) { prefs ->
                        prefs[WidgetTargetIdKey] = config.first
                        prefs[WidgetShortNamesKey] = config.second
                    }
                    ScheduleWidget().update(this@ScheduleWidgetApplyActivity, glanceId)
                }
            }
            withContext(Dispatchers.Main) {
                if (!isFinishing) finish()
            }
        }
    }

    companion object {
        /** Имя для Intent'а success-callback'а (WidgetPinning.android). */
        const val INTENT_NAME = "com.jetbrains.kmpapp.widget.ScheduleWidgetApplyActivity"
    }
}
