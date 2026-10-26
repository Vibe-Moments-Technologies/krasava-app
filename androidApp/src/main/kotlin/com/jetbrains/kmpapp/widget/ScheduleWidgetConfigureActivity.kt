package com.jetbrains.kmpapp.widget

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.jetbrains.kmpapp.data.widget.WidgetPayload
import com.jetbrains.kmpapp.data.widget.WidgetPayloadCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Настройка экземпляра виджета: выбор сохранённого расписания.
 *
 * Аналог iOS-конфигурации (`AppIntent` на iOS 17+). Выбор хранится в состоянии
 * Glance для конкретного `appWidgetId`, поэтому у каждого виджета на экране
 * может быть своя группа/преподаватель.
 */
class ScheduleWidgetConfigureActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val payload = WidgetPayloadCodec.decode(ScheduleWidgetStore.read(this@ScheduleWidgetConfigureActivity))
            val manager = GlanceAppWidgetManager(this@ScheduleWidgetConfigureActivity)
            val glanceId = runCatching { manager.getGlanceIdBy(appWidgetId) }.getOrNull()
            val storedTargetId = glanceId?.let { id ->
                runCatching {
                    getAppWidgetState(
                        this@ScheduleWidgetConfigureActivity,
                        PreferencesGlanceStateDefinition,
                        id
                    )[WidgetTargetIdKey]
                }.getOrNull()
            }
            withContext(Dispatchers.Main) {
                if (isFinishing || isDestroyed) return@withContext
                if (payload == null) {
                    showMessage("Откройте приложение и обновите расписание, чтобы выбрать его для виджета.")
                } else {
                    showTargetPicker(payload, storedTargetId, glanceId)
                }
            }
        }
    }

    private fun showTargetPicker(
        payload: WidgetPayload,
        storedTargetId: Int?,
        glanceId: androidx.glance.GlanceId?
    ) {
        val targets = payload.targets
        if (targets.isEmpty()) {
            showMessage("В приложении пока нет сохранённых расписаний.")
            return
        }

        val initial = targets.indexOfFirst {
            it.id == (storedTargetId ?: payload.defaultTargetId)
        }.takeIf { it >= 0 } ?: 0
        var selected = initial

        AlertDialog.Builder(this)
            .setTitle("Расписание для виджета")
            .setSingleChoiceItems(
                targets.map { "${it.title} · ${it.typeName}" }.toTypedArray(),
                initial
            ) { _, which -> selected = which }
            .setPositiveButton("Сохранить") { _, _ ->
                save(targets[selected].id, glanceId)
            }
            .setNegativeButton("Отмена") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun save(targetId: Int, glanceId: androidx.glance.GlanceId?) {
        val id = appWidgetId
        if (glanceId == null) {
            finish()
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                updateAppWidgetState(
                    this@ScheduleWidgetConfigureActivity,
                    glanceId
                ) { prefs ->
                    prefs[WidgetTargetIdKey] = targetId
                }
                ScheduleWidget().update(this@ScheduleWidgetConfigureActivity, glanceId)
            }
            withContext(Dispatchers.Main) {
                if (isFinishing || isDestroyed) return@withContext
                setResult(
                    RESULT_OK,
                    Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                )
                finish()
            }
        }
    }

    private fun showMessage(text: String) {
        AlertDialog.Builder(this)
            .setMessage(text)
            .setPositiveButton("Ок") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }
}
