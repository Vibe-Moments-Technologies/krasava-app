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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.data.model.HeaderButton
import com.jetbrains.kmpapp.screens.components.PlatformBackHandler
import com.jetbrains.kmpapp.screens.schedule.icon

/**
 * Общий каркас страниц-разделов настроек.
 *
 * Переработка (решение владельца): корень настроек — плоский список ссылок,
 * каждая ведёт на страницу раздела, а внутри уже группы настроек БЕЗ
 * вложенных подстраниц. Раньше было три уровня (Настройки → Расписание →
 * Отображение), и до «пустых пар» нужно было четыре тапа.
 *
 * Исключение одно: цвета занятий живут подстраницей внутри расписания —
 * палитра слишком крупная для инлайна.
 */
@Composable
internal fun SettingsPageScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    // Скролл живёт в ViewModel (ключ = экран): LayeredNavHost пересоздаёт
    // страницу в другом слое при уходе глубже — remember-состояние там не
    // выживает, а общий ScrollState сохраняет позицию (как в корне настроек).
    scrollKey: String? = null,
    content: @Composable () -> Unit
) {
    PlatformBackHandler(onBack = onBack)
    val scrollState = if (scrollKey != null) {
        org.koin.compose.viewmodel.koinViewModel<OtherViewModel>().scrollState(scrollKey)
    } else {
        rememberScrollState()
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
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/**
 * Группа настроек внутри страницы раздела: заголовок + карточка с рядами.
 *
 * Заголовок снаружи карточки — так группы читаются как разделы, а не как
 * ещё одна вложенность.
 */
@Composable
internal fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        GlitchText(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
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

/** Тумблер настройки. Разделитель рисуется вызывающей стороной между рядами. */
@Composable
internal fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    val alpha = if (enabled) 1f else 0.45f
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            GlitchText(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )
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
}

/** Разделитель между рядами внутри группы. */
@Composable
internal fun SettingsRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

/** Доступность кнопки шапки: контекстные требуют условий. */
internal fun HeaderButton.availableInSettings(
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

/** Полное название кнопки для настроек (в шапке места мало — там короткое). */
internal fun HeaderButton.settingsTitle(): String = when (this) {
    HeaderButton.ADD -> "Добавить расписание"
    HeaderButton.USER_LESSON -> "Своя пара"
    HeaderButton.QR -> "Сканер QR"
    HeaderButton.DAY_BADGE -> "Кружок дня"
}

internal fun HeaderButton.settingsSubtitle(available: Boolean): String = when (this) {
    HeaderButton.ADD -> "Плюс: добавить группу, преподавателя или аудиторию"
    HeaderButton.USER_LESSON -> if (available) {
        "Календарик: добавить свою пару в расписание"
    } else {
        "Нужно выбранное расписание и включённое «Редактирование расписания»"
    }
    HeaderButton.QR -> if (available) {
        "Отметка посещаемости на «Пульсе»"
    } else {
        "Включите «Сканер QR» в разделе «Сервисы»"
    }
    HeaderButton.DAY_BADGE -> if (available) {
        "Число дня: видно, когда лента календаря свёрнута"
    } else {
        "Нужно выбранное расписание"
    }
}

/** Строка-свитчер одной кнопки шапки (слот один — радио-группа). */
@Composable
internal fun HeaderButtonToggleRow(
    button: HeaderButton,
    checked: Boolean,
    enabled: Boolean,
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
                text = button.settingsSubtitle(enabled),
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
}

/**
 * Ряд-ссылка внутри страницы раздела.
 *
 * Нужен ровно для одного случая — «Цвета занятий» (согласованное исключение):
 * палитра типов слишком крупная, чтобы жить инлайн.
 */
@Composable
internal fun SettingsLinkRow(
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