package com.jetbrains.kmpapp.screens.other
import com.jetbrains.kmpapp.theme.GlitchText

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.data.widget.WidgetPinFeedback
import com.jetbrains.kmpapp.data.widget.isScheduleWidgetPinningSupported
import com.jetbrains.kmpapp.data.widget.requestScheduleWidgetPin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ждать открытия конфигуратора виджета после запроса закрепления, прежде
 * чем решить, что системный диалог был подавлен (OEM-запрет).
 */
private const val WIDGET_PIN_DIALOG_TIMEOUT_MS = 5_000L

/** «По умолчанию» в конфигураторе виджета: цель уведомлений (id < 0). */
private const val DEFAULT_WIDGET_TARGET_ID = -1

/**
 * Страница «Дополнительно».
 *
 * VPN-предупреждения и виджет переехали сюда из корня настроек.
 * Эксперименты и полный сброс сюда НЕ выносим (решение владельца): они
 * остаются в скрытом меню отладки.
 */
@Composable
fun AdvancedSettingsPage(
    viewModel: OtherViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vpnWarningEnabled by viewModel.vpnWarningEnabled.collectAsState()
    val savedTargets by viewModel.savedTargets.collectAsState()
    val notificationsTargetId by viewModel.notificationsTargetId.collectAsState()

    var showWidgetConfigDialog by remember { mutableStateOf(false) }
    var widgetConfigTargetId by remember { mutableStateOf<Int?>(null) }
    var widgetConfigShortNames by remember { mutableStateOf(false) }
    var showWidgetPinFailedDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    SettingsPageScaffold(title = "Дополнительно", onBack = onBack, modifier = modifier) {
        SettingsGroup(title = "Сеть") {
            SettingsSwitchRow(
                title = "Предупреждения о VPN",
                subtitle = "Показывать предупреждение, если VPN может помешать обновлению расписания",
                checked = vpnWarningEnabled,
                onCheckedChange = viewModel::setVpnWarningEnabled
            )
        }

        if (isScheduleWidgetPinningSupported()) {
            SettingsGroup(title = "Виджет") {
                SettingsLinkRow(
                    title = "Добавить на главный экран",
                    subtitle = "Выберите расписание — система предложит разместить виджет",
                    icon = Icons.Default.Add,
                    onClick = { showWidgetConfigDialog = true }
                )
                Spacer(modifier = Modifier.height(6.dp))
                GlitchText(
                    text = "Убрать виджет — долгим нажатием по нему → «Удалить».",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showWidgetConfigDialog) {
        // Внутренний конфигуратор: лаунчеры не показывают системный при
        // добавлении из приложения — параметры выбираем здесь.
        AlertDialog(
            onDismissRequest = { showWidgetConfigDialog = false },
            title = { GlitchText("Виджет расписания") },
            text = {
                if (savedTargets.isEmpty()) {
                    GlitchText("Сначала добавьте хотя бы одно расписание.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        GlitchText(
                            text = "Расписание",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        val effectiveId = widgetConfigTargetId
                            ?: notificationsTargetId
                            ?: savedTargets.firstOrNull()?.id
                        savedTargets.forEach { target ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { widgetConfigTargetId = target.id }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    GlitchText(
                                        text = target.fullTitle,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                RadioButton(
                                    selected = target.id == effectiveId,
                                    onClick = { widgetConfigTargetId = target.id }
                                )
                            }
                        }
                        GlitchText(
                            text = "«По умолчанию» = расписание уведомлений. Виджеты без своего выбора берут его же.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { widgetConfigShortNames = !widgetConfigShortNames }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                GlitchText(
                                    text = "Сокращать длинные названия",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                GlitchText(
                                    text = "Инициалы слов: «Базы данных и управление ими» → «БДиУИ»",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Checkbox(
                                checked = widgetConfigShortNames,
                                onCheckedChange = { widgetConfigShortNames = it }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWidgetConfigDialog = false
                        if (savedTargets.isEmpty()) return@TextButton
                        val chosen = widgetConfigTargetId
                            ?: notificationsTargetId
                            ?: savedTargets.firstOrNull()?.id
                        val targetId = chosen ?: DEFAULT_WIDGET_TARGET_ID
                        WidgetPinFeedback.setPendingConfig(targetId, widgetConfigShortNames)
                        if (requestScheduleWidgetPin()) {
                            WidgetPinFeedback.markPinRequested()
                            val requestedAt = WidgetPinFeedback.lastPinRequestedAtMillis
                            scope.launch {
                                delay(WIDGET_PIN_DIALOG_TIMEOUT_MS)
                                if (WidgetPinFeedback.lastConfigureOpenedAtMillis < requestedAt) {
                                    showWidgetPinFailedDialog = true
                                }
                            }
                        } else {
                            showWidgetPinFailedDialog = true
                        }
                    }
                ) { GlitchText("Добавить виджет") }
            },
            dismissButton = {
                TextButton(onClick = { showWidgetConfigDialog = false }) { GlitchText("Отмена") }
            }
        )
    }

    if (showWidgetPinFailedDialog) {
        AlertDialog(
            onDismissRequest = { showWidgetPinFailedDialog = false },
            title = { GlitchText("Не удалось добавить виджет") },
            text = {
                GlitchText(
                    "Добавьте его вручную: долгое нажатие на рабочем столе → «Виджеты» → «Красава» → «Расписание»."
                )
            },
            confirmButton = {
                TextButton(onClick = { showWidgetPinFailedDialog = false }) { GlitchText("Понятно") }
            }
        )
    }
}