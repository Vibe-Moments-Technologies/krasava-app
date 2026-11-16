package com.jetbrains.kmpapp.screens.other

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.theme.GlitchText

@Composable
fun CoffeeThemeUnlockDialog(
    themeEnabled: Boolean,
    onThemeEnabledChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            GlitchText(
                text = "Кофейная пауза",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                GlitchText(
                    text = "Вы заглянули к каждому из четырёх разработчиков. " +
                        "Самое время выдохнуть, заварить кофе и включить ламповую " +
                        "кофейную тему — тёплые тона, мягкий свет и ноль спешки.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        GlitchText(
                            text = "Кофейная тема",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        GlitchText(
                            text = if (themeEnabled) "Уже включена" else "Включить сейчас",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = themeEnabled,
                        onCheckedChange = onThemeEnabledChange
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                GlitchText(if (themeEnabled) "Насладиться" else "Хорошо")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                GlitchText("Позже")
            }
        }
    )
}
