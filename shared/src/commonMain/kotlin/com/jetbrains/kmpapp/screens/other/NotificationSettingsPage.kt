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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.data.notifications.NotificationsManager

/**
 * Страница «Уведомления».
 *
 * Блок переехал из корня настроек целиком (переработка: корень — плоский
 * список разделов). Логика системных разрешений сохранена как была:
 * тумблер не врёт и включается только при реально выданном праве.
 */
@Composable
fun NotificationSettingsPage(
    viewModel: OtherViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val notifyMinutesBefore by viewModel.notifyMinutesBefore.collectAsState()
    val notificationsTargetId by viewModel.notificationsTargetId.collectAsState()
    val savedTargets by viewModel.savedTargets.collectAsState()

    var showDeniedDialog by remember { mutableStateOf(false) }
    var showTargetDialog by remember { mutableStateOf(false) }
    var showCustomMinutesDialog by remember { mutableStateOf(false) }
    var customMinutesDraft by remember { mutableStateOf("") }

    SettingsPageScaffold(title = "Уведомления", onBack = onBack, modifier = modifier) {
        // Раньше блок целиком прятался при отсутствии платформенного движка.
        // Теперь раздел виден всегда, поэтому объясняем, почему пусто —
        // иначе страница выглядит сломанной.
        if (!NotificationsManager.supportsNotifications) {
            SettingsGroup(title = "Недоступно") {
                GlitchText(
                    text = "На этой платформе напоминания о занятиях не поддерживаются.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@SettingsPageScaffold
        }
        SettingsGroup(title = "Напоминания") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    GlitchText(
                        text = "Напоминать о занятиях",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    GlitchText(
                        text = "Локальное напоминание до начала пары выбранного расписания. Работает без интернета, прямо на устройстве",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { wanted ->
                        // Статус читается в момент клика (без remember-кэша):
                        // после выдачи прав повторное включение проходит сразу.
                        if (wanted && !NotificationsManager.isAuthorized()) {
                            NotificationsManager.requestAuthorization()
                            showDeniedDialog = true
                        } else {
                            viewModel.setNotificationsEnabled(wanted)
                        }
                    }
                )
            }
            SettingsRowDivider()
            // Выбор цели доступен всегда: она же дефолт для виджетов.
            val notificationTarget = savedTargets.firstOrNull { it.id == notificationsTargetId }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showTargetDialog = true }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    GlitchText(
                        text = "Расписание для уведомлений",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    GlitchText(
                        text = notificationTarget?.fullTitle ?: "Выберите расписание",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Выбрать расписание",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SettingsGroup(title = "Время") {
            val presets = listOf(5, 10, 15)
            val isCustom = notifyMinutesBefore !in presets

            GlitchText(
                text = "За сколько минут до пары",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { minutes ->
                    FilterChip(
                        selected = notifyMinutesBefore == minutes,
                        onClick = { viewModel.setNotifyMinutesBefore(minutes) },
                        label = {
                            GlitchText(
                                text = "$minutes мин",
                                fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
                FilterChip(
                    selected = isCustom,
                    onClick = {
                        customMinutesDraft = if (isCustom) notifyMinutesBefore.toString() else "20"
                        showCustomMinutesDialog = true
                    },
                    label = {
                        GlitchText(
                            text = "Своё",
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }
            if (isCustom) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlitchText(
                        text = "Своё: $notifyMinutesBefore мин",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = {
                            customMinutesDraft = notifyMinutesBefore.toString()
                            showCustomMinutesDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Изменить время")
                    }
                }
            }
        }
    }

    if (showDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showDeniedDialog = false },
            title = { GlitchText("Уведомления не разрешены") },
            text = {
                GlitchText(
                    "Напоминания включатся, только когда система разрешит приложению показывать уведомления. " +
                        "Разрешите их для «Красавы» в настройках телефона и вернитесь включить напоминания."
                )
            },
            confirmButton = {
                TextButton(onClick = { showDeniedDialog = false }) { GlitchText("Понятно") }
            }
        )
    }

    if (showTargetDialog) {
        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            title = { GlitchText("Расписание для уведомлений") },
            text = {
                if (savedTargets.isEmpty()) {
                    GlitchText("Сначала добавьте хотя бы одно расписание.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        savedTargets.forEach { target ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setNotificationsTargetId(target.id)
                                        showTargetDialog = false
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    GlitchText(
                                        text = target.fullTitle,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    GlitchText(
                                        text = target.displayTypeName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (target.id == notificationsTargetId) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Выбрано",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTargetDialog = false }) { GlitchText("Закрыть") }
            }
        )
    }

    if (showCustomMinutesDialog) {
        AlertDialog(
            onDismissRequest = { showCustomMinutesDialog = false },
            title = { GlitchText("Своё время") },
            text = {
                OutlinedTextField(
                    value = customMinutesDraft,
                    onValueChange = { text ->
                        customMinutesDraft = text.filter { it.isDigit() }.take(3)
                    },
                    label = { GlitchText("Минут до пары (1–120)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        customMinutesDraft.toIntOrNull()
                            ?.takeIf { it in 1..120 }
                            ?.let(viewModel::setNotifyMinutesBefore)
                        showCustomMinutesDialog = false
                    }
                ) { GlitchText("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomMinutesDialog = false }) { GlitchText("Отменить") }
            }
        )
    }
}