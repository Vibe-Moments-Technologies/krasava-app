package com.jetbrains.kmpapp.screens.other
import com.jetbrains.kmpapp.theme.GlitchText

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.data.model.HeaderButton
import com.jetbrains.kmpapp.screens.components.PlatformBackHandler
import com.jetbrains.kmpapp.screens.schedule.icon

// ── Расписание: отображение ──────────────────────────────────

@Composable
fun ScheduleDisplaySettingsScreen(
    viewModel: OtherViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlatformBackHandler(onBack = onBack)
    val showEmptyLessons by viewModel.showEmptyLessons.collectAsState()
    val hideAdditionalLessons by viewModel.hideAdditionalLessons.collectAsState()
    val showAbbreviatedNames by viewModel.showAbbreviatedNames.collectAsState()
    val autoScrollToCurrentLesson by viewModel.autoScrollToCurrentLesson.collectAsState()
    val userLessonsEnabled by viewModel.userLessonsEnabled.collectAsState()

    SettingsSubScreen(
        title = "Отображение расписания",
        onBack = onBack,
        modifier = modifier
    ) {
        SettingsToggleRow(
            title = "Редактирование расписания",
            subtitle = "Свои пары: кнопка в шапке и добавленные пары в списке",
            checked = userLessonsEnabled,
            onCheckedChange = { viewModel.setUserLessonsEnabled(it) }
        )
        SettingsToggleRow(
            title = "Показывать пустые пары",
            subtitle = "Отображать окна между занятиями в списке пар",
            checked = showEmptyLessons,
            onCheckedChange = { viewModel.setShowEmptyLessons(it) }
        )
        SettingsToggleRow(
            title = "Скрывать доп. занятия",
            subtitle = "Не показывать пары типа «ДОП» в расписании, календаре и напоминаниях",
            checked = hideAdditionalLessons,
            onCheckedChange = { viewModel.setHideAdditionalLessons(it) }
        )
        SettingsToggleRow(
            title = "Сокращённые названия",
            subtitle = "Сокращать по первым буквам названия предметов",
            checked = showAbbreviatedNames,
            onCheckedChange = { viewModel.setShowAbbreviatedNames(it) }
        )
        SettingsToggleRow(
            title = "Прокрутка к текущей паре",
            subtitle = "Фокусировать список на текущей паре при открытии",
            checked = autoScrollToCurrentLesson,
            onCheckedChange = { viewModel.setAutoScrollToCurrentLesson(it) }
        )
    }
}

// ── Расписание: прогресс ─────────────────────────────────────

@Composable
fun ScheduleProgressSettingsScreen(
    viewModel: OtherViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlatformBackHandler(onBack = onBack)
    val showLessonProgress by viewModel.showLessonProgress.collectAsState()
    val showEmptyLessonProgress by viewModel.showEmptyLessonProgress.collectAsState()
    val showBreakProgress by viewModel.showBreakProgress.collectAsState()

    SettingsSubScreen(
        title = "Прогресс и индикаторы",
        onBack = onBack,
        modifier = modifier
    ) {
        SettingsToggleRow(
            title = "Индикатор прогресса пары",
            subtitle = "Полоска оставшегося времени в карточке идущего занятия",
            checked = showLessonProgress,
            onCheckedChange = { viewModel.setShowLessonProgress(it) }
        )
        SettingsToggleRow(
            title = "Прогресс пустых пар",
            subtitle = "Полоска оставшегося времени в карточке «Нет пары»",
            checked = showEmptyLessonProgress,
            onCheckedChange = { viewModel.setShowEmptyLessonProgress(it) }
        )
        SettingsToggleRow(
            title = "Прогресс перемены",
            subtitle = "Пройденная часть надписи «перемена» подсвечивается цветом",
            checked = showBreakProgress,
            onCheckedChange = { viewModel.setShowBreakProgress(it) }
        )
    }
}

// ── Расписание: календарь ────────────────────────────────────

@Composable
fun ScheduleCalendarSettingsScreen(
    viewModel: OtherViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlatformBackHandler(onBack = onBack)
    val calendarSwipeCollapse by viewModel.calendarSwipeCollapse.collectAsState()

    SettingsSubScreen(
        title = "Календарь",
        onBack = onBack,
        modifier = modifier
    ) {
        SettingsToggleRow(
            title = "Сворачивание календаря свайпом",
            subtitle = "Свайп вверх по ленте календаря скрывает её над расписанием",
            checked = calendarSwipeCollapse,
            onCheckedChange = { viewModel.setCalendarSwipeCollapse(it) }
        )
    }
}

// ── Расписание: кнопки шапки ─────────────────────────────────

/**
 * Какая кнопка стоит в шапке расписания рядом с меню.
 *
 * Владелец: в шапке стабильно помещаются только две кнопки справа, поэтому
 * слот ОДИН. Включение другой кнопки автоматически выключает предыдущую —
 * свитчеры здесь работают как радио-группа, хотя выглядят как вкл/выкл.
 * Кнопка меню («⋮») в список не входит: она показывается всегда.
 */
@Composable
fun HeaderButtonsSettingsScreen(
    viewModel: OtherViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlatformBackHandler(onBack = onBack)
    val slotButton by viewModel.headerSlotButton.collectAsState()
    val userLessonsEnabled by viewModel.userLessonsEnabled.collectAsState()
    val qrButtonEnabled by viewModel.qrButtonEnabled.collectAsState()
    val savedTargets by viewModel.savedTargets.collectAsState()
    val hasTarget = savedTargets.isNotEmpty()

    SettingsSubScreen(
        title = "Кнопки шапки",
        onBack = onBack,
        modifier = modifier
    ) {
        HeaderButton.entries.forEach { button ->
            val available = button.availableInSettings(
                hasTarget = hasTarget,
                userLessonsEnabled = userLessonsEnabled,
                qrButtonEnabled = qrButtonEnabled
            )
            HeaderButtonToggleRow(
                button = button,
                checked = slotButton == button && available,
                enabled = available,
                subtitle = button.settingsSubtitle(available),
                onCheckedChange = { checked ->
                    // Слот один: включить можно только что-то одно, поэтому
                    // «выключение» переводит слот на дефолтную кнопку «плюс».
                    viewModel.setHeaderSlotButton(
                        if (checked) button else HeaderButton.DEFAULT
                    )
                }
            )
        }
    }
}

/** Доступна ли кнопка в настройках (контекстные требуют условий). */
private fun HeaderButton.availableInSettings(
    hasTarget: Boolean,
    userLessonsEnabled: Boolean,
    qrButtonEnabled: Boolean
): Boolean = when (this) {
    HeaderButton.ADD -> true
    HeaderButton.USER_LESSON -> hasTarget && userLessonsEnabled
    HeaderButton.QR -> qrButtonEnabled
    // Кружок дня виден только при свёрнутой ленте, но поставить его в слот
    // можно заранее — когда лента свернётся, число появится в шапке.
    HeaderButton.DAY_BADGE -> hasTarget
}

private fun HeaderButton.settingsSubtitle(available: Boolean): String = when (this) {
    HeaderButton.ADD -> "Плюс: добавить группу, преподавателя или аудиторию"
    HeaderButton.USER_LESSON -> if (available) {
        "Календарик: добавить свою пару в расписание"
    } else {
        "Нужно выбранное расписание и включённое «Редактирование расписания»"
    }
    HeaderButton.QR -> if (available) {
        "Отметка посещаемости на «Пульсе»"
    } else {
        "Включите «Кнопку сканера QR» в разделе «Дополнительный функционал»"
    }
    HeaderButton.DAY_BADGE -> if (available) {
        "Число дня: видно, когда лента календаря свёрнута"
    } else {
        "Нужно выбранное расписание"
    }
}

/** Строка-свитчер одной кнопки шапки. */
@Composable
private fun HeaderButtonToggleRow(
    button: HeaderButton,
    checked: Boolean,
    enabled: Boolean,
    subtitle: String,
    onCheckedChange: (Boolean) -> Unit
) {
    val alpha = if (enabled) 1f else 0.45f
    val buttonIcon = button.icon()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (buttonIcon != null) {
                    Icon(
                        imageVector = buttonIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                GlitchText(
                    text = button.settingsTitle(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            GlitchText(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null
        )
    }
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

/** Полное название кнопки для настроек (в шапке места мало — там короткое). */
private fun HeaderButton.settingsTitle(): String = when (this) {
    HeaderButton.ADD -> "Добавить расписание"
    HeaderButton.USER_LESSON -> "Своя пара"
    HeaderButton.QR -> "Сканер QR"
    HeaderButton.DAY_BADGE -> "Кружок дня"
}

// ── Параметры сервисов: Конспекты ────────────────────────────

@Composable
fun ServiceNotesSettingsScreen(
    viewModel: OtherViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlatformBackHandler(onBack = onBack)
    val askBeforeNoteDelete by viewModel.askBeforeNoteDelete.collectAsState()

    SettingsSubScreen(
        title = "Конспекты",
        onBack = onBack,
        modifier = modifier
    ) {
        SettingsToggleRow(
            title = "Спрашивать при удалении",
            subtitle = "Подтверждение перед удалением страниц и полей в конспектах",
            checked = askBeforeNoteDelete,
            onCheckedChange = { viewModel.setAskBeforeNoteDelete(it) }
        )
    }
}

// ── Общие композаблы ─────────────────────────────────────────

@Composable
private fun SettingsSubScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
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
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад"
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                GlitchText(
                    text = title,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun ColumnScope_SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// Обёртка для вызова из Column
@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ColumnScope_SettingsToggleRow(title, subtitle, checked, onCheckedChange)
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}
