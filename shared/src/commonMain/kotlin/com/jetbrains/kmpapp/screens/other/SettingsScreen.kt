package com.jetbrains.kmpapp.screens.other
import com.jetbrains.kmpapp.theme.GlitchText

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.data.appicon.AppIconManager
import com.jetbrains.kmpapp.data.model.ThemeMode
import com.jetbrains.kmpapp.screens.components.PlatformBackHandler

/**
 * Корень «Настройки» — плоский список разделов.
 *
 * Переработка (решение владельца): раньше здесь было 8 блоков и 9 ссылок на
 * подстраницы, а внутри «Расписания» — ещё четыре ссылки (три уровня, четыре
 * тапа до тумблера). Теперь:
 *  - «Внешний вид» остаётся инлайн, как и был;
 *  - остальные разделы — по одной ссылке на страницу с группами настроек.
 *
 * Настройки игр живут в шестерёнке внутри «Игр», эксперименты и полный сброс —
 * только в скрытом меню отладки (решения владельца).
 */
@Composable
fun SettingsScreen(
    viewModel: OtherViewModel,
    onBack: () -> Unit,
    onOpenScheduleSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenInterfaceSettings: () -> Unit,
    onOpenServicesSettings: () -> Unit,
    onOpenDataAndCache: () -> Unit,
    onOpenAdvancedSettings: () -> Unit,
    onOpenIconPicker: () -> Unit = {},
    onOpenThemedThemes: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    PlatformBackHandler(onBack = onBack)

    val themeMode by viewModel.themeMode.collectAsState()
    val isKawaiiTheme by viewModel.isKawaiiTheme.collectAsState()

    var sakuraTapCount by remember { mutableIntStateOf(0) }
    var lastSakuraTapMark by remember { mutableStateOf<kotlin.time.TimeMark?>(null) }
    var showKawaiiDialog by remember { mutableStateOf(false) }

    // Скролл живёт в ViewModel: LayeredNavHost пересоздаёт экран в другом
    // слое при переходе в раздел — общий ScrollState переживает пересоздание.
    val scrollState = viewModel.settingsScrollState

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
                GlitchText(
                    text = "Настройки",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Внешний вид: остаётся инлайн (решение владельца) ──
            SettingsSectionCard(
                title = "Внешний вид",
                icon = Icons.Default.Palette,
                onIconClick = {
                    val mark = lastSakuraTapMark
                    if (mark != null && mark.elapsedNow().inWholeMilliseconds < 1500L) {
                        sakuraTapCount++
                    } else {
                        sakuraTapCount = 1
                    }
                    lastSakuraTapMark = kotlin.time.TimeSource.Monotonic.markNow()
                    if (sakuraTapCount >= 8) {
                        sakuraTapCount = 0
                        // Квест из восьми быстрых тычков по иконке раздела
                        // выдаёт «Кавай». Блок остался на месте — квест жив.
                        showKawaiiDialog = true
                    }
                }
            ) {
                GlitchText(
                    text = "Тема приложения",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                GlitchText(
                    text = "Выберите желаемый стиль интерфейса",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = {
                                GlitchText(
                                    text = mode.displayName,
                                    fontSize = 13.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (AppIconManager.supportsSwitching) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsNavigationRow(
                        title = "Иконка приложения",
                        subtitle = "Новая или старая · тема — автоматически",
                        onClick = onOpenIconPicker
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                SettingsNavigationRow(
                    title = "Тематические темы",
                    subtitle = "Сакура и квестовые темы — найденные и скрытые",
                    onClick = onOpenThemedThemes
                )
            }

            // ── Разделы: по одной ссылке, внутри — группы настроек ──
            SettingsSectionCard(title = "Разделы", icon = Icons.Default.Tune) {
                SettingsNavigationRow(
                    title = "Настройки расписания",
                    subtitle = "Отображение, прогресс, календарь, кнопки шапки, цвета",
                    onClick = onOpenScheduleSettings,
                    icon = Icons.Default.CalendarMonth
                )
                SettingsSectionDivider()
                SettingsNavigationRow(
                    title = "Уведомления",
                    subtitle = "Напоминания о парах и время до начала",
                    onClick = onOpenNotificationSettings,
                    icon = Icons.Default.Notifications
                )
                SettingsSectionDivider()
                SettingsNavigationRow(
                    title = "Интерфейс и навигация",
                    subtitle = "Состав и порядок нижней панели",
                    onClick = onOpenInterfaceSettings,
                    icon = Icons.Default.Tune
                )
                SettingsSectionDivider()
                SettingsNavigationRow(
                    title = "Сервисы",
                    subtitle = "Конспекты, сканер QR, задачи",
                    onClick = onOpenServicesSettings,
                    icon = Icons.Default.Extension
                )
                SettingsSectionDivider()
                SettingsNavigationRow(
                    title = "Данные и память",
                    subtitle = "Что занимает место на устройстве",
                    onClick = onOpenDataAndCache,
                    icon = Icons.Default.Storage
                )
                SettingsSectionDivider()
                SettingsNavigationRow(
                    title = "Дополнительно",
                    subtitle = "Предупреждения о VPN, виджет на главный экран",
                    onClick = onOpenAdvancedSettings,
                    icon = Icons.Default.Tune
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showKawaiiDialog) {
        AlertDialog(
            onDismissRequest = { showKawaiiDialog = false },
            title = { GlitchText("Кавай 🎀", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    GlitchText(
                        text = "Очередной тыкальщик? 😏\n\nРаз уж ты нашёл этот секрет — держи эксклюзивную тему «Кавай»: полностью анимешная, милая, в бело-розовых тонах!",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlitchText(
                            text = "Кавай",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(
                            checked = isKawaiiTheme,
                            onCheckedChange = { viewModel.setKawaiiTheme(it) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showKawaiiDialog = false }) { GlitchText("Готово") }
            }
        )
    }
}

@Composable
private fun SettingsSectionDivider() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun SettingsNavigationRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column {
                GlitchText(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                GlitchText(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Открыть",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    onIconClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(22.dp)
                        .then(
                            if (onIconClick != null) Modifier.clickable(onClick = onIconClick)
                            else Modifier
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                GlitchText(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}