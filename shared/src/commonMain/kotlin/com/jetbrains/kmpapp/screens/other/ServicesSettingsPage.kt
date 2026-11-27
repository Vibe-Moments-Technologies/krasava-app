package com.jetbrains.kmpapp.screens.other
import com.jetbrains.kmpapp.theme.GlitchText

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Страница «Сервисы».
 *
 * Настройки отдельных сервисов собраны здесь группами: раньше «Конспекты»
 * были отдельной подстраницей ради ОДНОГО тумблера, а QR лежал в блоке
 * «Дополнительный функционал» рядом с VPN.
 *
 * Настройки игр сюда НЕ переносим (решение владельца): они живут в
 * шестерёнке внутри самого сервиса «Игры» — это настройка игры, а не
 * приложения.
 */
@Composable
fun ServicesSettingsPage(
    viewModel: OtherViewModel,
    onBack: () -> Unit,
    onOpenTasks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val askBeforeNoteDelete by viewModel.askBeforeNoteDelete.collectAsState()

    SettingsPageScaffold(title = "Сервисы", onBack = onBack, modifier = modifier) {
        SettingsGroup(title = "Конспекты") {
            SettingsSwitchRow(
                title = "Спрашивать при удалении",
                subtitle = "Подтверждение перед удалением страниц и полей в конспектах",
                checked = askBeforeNoteDelete,
                onCheckedChange = viewModel::setAskBeforeNoteDelete
            )
        }

        SettingsGroup(title = "Задачи и дедлайны") {
            SettingsLinkRow(
                title = "Настройки задач",
                subtitle = "Раздел в разработке",
                onClick = onOpenTasks
            )
        }

        SettingsGroup(title = "Игры") {
            GlitchText(
                text = "Настройки «Сапёра» и «Тетриса» открываются шестерёнкой внутри самих игр.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Заглушка «Задачи и дедлайны» (решение владельца): раздел возвращён в
 * навигацию, но настройки ещё не готовы — показываем техработы по центру.
 */
@Composable
fun TasksSettingsStubScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsPageScaffold(title = "Задачи и дедлайны", onBack = onBack, modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Engineering,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(72.dp)
                )
                GlitchText(
                    text = "Раздел в разработке",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                GlitchText(
                    text = "Настройки задач и дедлайнов появятся в одном из следующих обновлений.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}