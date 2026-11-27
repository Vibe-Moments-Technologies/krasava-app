package com.jetbrains.kmpapp.screens.other
import com.jetbrains.kmpapp.theme.GlitchText

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.data.model.HeaderButton
import com.jetbrains.kmpapp.screens.schedule.icon

/**
 * Подстраница «Кнопки шапки» (переработка настроек).
 *
 * Владелец: выбрать до ТРЁХ любых кнопок, включая кнопку меню. В пузырьке
 * показываются все НЕ выбранные кнопки. QR больше не имеет отдельного
 * тумблера — включается/выключается здесь наравне с остальными.
 */
@Composable
fun HeaderButtonsSettingsPage(
    viewModel: OtherViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selected by viewModel.headerButtons.collectAsState()

    SettingsPageScaffold(
        title = "Кнопки шапки",
        onBack = onBack,
        modifier = modifier,
        scrollKey = "header_buttons"
    ) {
        SettingsGroup(title = "В шапке расписания") {
            GlitchText(
                text = "Выберите до ${HeaderButton.MAX_VISIBLE} кнопок. Остальные появятся в меню.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            HeaderButton.entries.forEachIndexed { index, button ->
                if (index > 0) SettingsRowDivider()
                val checked = button in selected
                val canCheck = checked || selected.size < HeaderButton.MAX_VISIBLE
                HeaderButtonChoiceRow(
                    button = button,
                    checked = checked,
                    enabled = canCheck,
                    onCheckedChange = { wantChecked ->
                        val next = if (wantChecked) {
                            (selected + button).distinct().take(HeaderButton.MAX_VISIBLE)
                        } else {
                            selected - button
                        }
                        viewModel.setHeaderButtons(next)
                    }
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            TextButton(onClick = { viewModel.setHeaderButtons(HeaderButton.DEFAULT_SELECTION) }) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                GlitchText("Сбросить на «Добавить + Меню»")
            }
        }
    }
}

@Composable
private fun HeaderButtonChoiceRow(
    button: HeaderButton,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val alpha = if (enabled) 1f else 0.45f
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            val icon = button.icon()
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column {
                GlitchText(
                    text = button.settingsTitle(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
                Spacer(modifier = Modifier.height(2.dp))
                GlitchText(
                    text = button.settingsSubtitle(enabled),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )
            }
        }
        Checkbox(checked = checked, onCheckedChange = if (enabled) onCheckedChange else null)
    }
}
