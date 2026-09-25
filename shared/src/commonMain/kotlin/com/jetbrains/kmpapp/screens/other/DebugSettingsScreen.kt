package com.jetbrains.kmpapp.screens.other

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.data.DebugConfig
import com.jetbrains.kmpapp.data.analytics.AppDiagnostics
import com.jetbrains.kmpapp.data.model.AppVersion
import com.jetbrains.kmpapp.data.notifications.NotificationsManager
import com.jetbrains.kmpapp.screens.components.PlatformBackHandler

@Composable
fun DebugSettingsScreen(
    viewModel: OtherViewModel,
    onBack: () -> Unit,
    onOpenExperimentalSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlatformBackHandler(onBack = onBack)
    val simulateOffline by DebugConfig.isOfflineSimulated.collectAsState()
    val mapCoordinatePlane by DebugConfig.isMapCoordinatePlaneEnabled.collectAsState()
    val diagnosticsEnabled by viewModel.diagnosticsEnabled.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var testEventSent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshStorageStats()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Отладка и эксперименты",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        modifier = modifier
            .fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(viewModel.scrollState("debug"))
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle("Режимы")
            DebugSwitchCard(
                title = "Имитировать оффлайн",
                subtitle = "Использовать сохранённые данные без сети",
                checked = simulateOffline,
                onCheckedChange = DebugConfig::setOfflineSimulated
            )
            DebugSwitchCard(
                title = "Координатная плоскость на картах",
                subtitle = "Сетка координат поверх карт для отладки геометрии",
                checked = mapCoordinatePlane,
                onCheckedChange = DebugConfig::setMapCoordinatePlaneEnabled
            )

            // Диагностика (Sentry): opt-in для тестировщиков. Выключена —
            // наружу не уходит ничего вообще. На тестовых каналах (dev/beta/
            // contrib) сбор обязателен: тумблер заблокирован во включённом состоянии.
            SectionTitle("Диагностика")
            DebugSwitchCard(
                title = "Отправка диагностики",
                subtitle = if (AppDiagnostics.isForced) {
                    "Тестовая сборка (канал ${AppVersion.BUILD_CHANNEL}): диагностика включена всегда."
                } else {
                    "Краши и ошибки уходят в Sentry. Только для тестировщиков: " +
                        "включайте на время отладки, данные покидают устройство."
                },
                checked = diagnosticsEnabled || AppDiagnostics.isForced,
                enabled = !AppDiagnostics.isForced,
                onCheckedChange = viewModel::setDiagnosticsEnabled
            )
            DebugCard {
                Text(
                    "Проверка связи",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Отправляет тестовое событие в Sentry. Через минуту оно появится в проекте — " +
                        "значит, канал диагностики живой.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        AppDiagnostics.sendTestEvent()
                        testEventSent = true
                    },
                    enabled = !testEventSent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (testEventSent) "Отправлено ✓" else "Отправить тестовое событие")
                }
            }

            SectionTitle("Хранилище")
            DebugCard {
                Text(
                    "Расписаний: ${storageStats.schedulesCount} · пар: ${storageStats.lessonsCount} · размер: ${storageStats.formatBytes(storageStats.totalSizeBytes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { showClearCacheDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Очистить кеш расписаний")
                }
                Text(
                    "Сохранённые группы и настройки останутся.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (NotificationsManager.supportsNotifications) {
                SectionTitle("Уведомления")
                DebugCard {
                    Text(
                        "Мгновенная доставка или будильник через минуту.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { NotificationsManager.sendTest(1_500L) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Отправить сейчас")
                    }
                    OutlinedButton(
                        onClick = { NotificationsManager.sendTest(60_000L) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Запланировать через 1 минуту")
                    }
                }
            }

            SectionTitle("Эксперименты")
            DebugCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenExperimentalSettings),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Экспериментальные параметры",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Скрытые возможности и секреты",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Открыть",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SectionTitle("Опасная зона")
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(10.dp))
                        Text("Полный сброс", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "Удалит расписания, задачи, кеши и все настройки. Приложение вернётся к состоянию после установки.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Button(onClick = { showResetDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Сбросить всё")
                    }
                }
            }
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Очистить кеш расписаний?") },
            text = { Text("Сохранённые расписания будут загружены заново при следующем обновлении.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearCache()
                    showClearCacheDialog = false
                }) { Text("Очистить") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearCacheDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Сбросить все данные?") },
            text = { Text("Это удалит все сохранённые данные и настройки без возможности восстановления.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.resetAllData()
                    showResetDialog = false
                    onBack()
                }) { Text("Сбросить всё") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetDialog = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
private fun DebugCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun DebugSwitchCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    DebugCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}
