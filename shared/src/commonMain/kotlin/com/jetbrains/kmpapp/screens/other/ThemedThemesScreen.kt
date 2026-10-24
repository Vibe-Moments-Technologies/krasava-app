package com.jetbrains.kmpapp.screens.other

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.screens.components.PlatformBackHandler
import com.jetbrains.kmpapp.theme.GlitchText
import com.jetbrains.kmpapp.theme.ThemeOverlay

/**
 * Подстраница «Тематические темы»: тумблеры найденных тем-надстроек.
 *
 * Состав списка честный и простой:
 * - Сакура — всегда: розовая тема доступна по умолчанию и включена сразу,
 *   искать её больше не нужно;
 * - Кавай, Киберпанк, Матрица — только после того, как пользователь нашёл
 *   их квестами. Разблокировка сохраняется, повторно проходить квест не
 *   нужно; потерянная Матрица исчезает из списка навсегда.
 *
 * Хэллоуинской темы здесь нет: она сезонная (28 октября — 3 ноября) и
 * управляется собственной логикой плюс тумблером в отладке.
 *
 * Темы взаимно исключают друг друга: включение одной выключает остальные —
 * это свойство оверлея, а не списка.
 */
@Composable
fun ThemedThemesScreen(
    viewModel: OtherViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlatformBackHandler(onBack = onBack)

    val isSakura by viewModel.isSakuraTheme.collectAsState()
    val isKawaii by viewModel.isKawaiiTheme.collectAsState()
    val isCyberpunk by viewModel.isCyberpunkTheme.collectAsState()
    val isMatrix by viewModel.isMatrixTheme.collectAsState()
    val unlocked by viewModel.unlockedThemes.collectAsState()

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
                    text = "Тематические темы",
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
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(viewModel.scrollState("themed_themes"))
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    ThemeRow(
                        emoji = "🌸",
                        title = "Сакура",
                        subtitle = "Пастельно-розовая. Доступна всегда, без поисков",
                        checked = isSakura,
                        onCheckedChange = viewModel::setSakuraTheme
                    )

                    // Квестовые темы: строка появляется только после того,
                    // как тема найдена. Порядок фиксированный, чтобы список
                    // не прыгал при разблокировках.
                    val questRows = listOfNotNull(
                        if (ThemeOverlay.KAWAII in unlocked) ThemeRowData(
                            emoji = "🎀",
                            title = "Кавай",
                            subtitle = "Анимешная и милая, в бело-розовых тонах",
                            checked = isKawaii,
                            onCheckedChange = viewModel::setKawaiiTheme
                        ) else null,
                        if (ThemeOverlay.CYBERPUNK in unlocked) ThemeRowData(
                            emoji = "⚡",
                            title = "Киберпанк",
                            subtitle = "Неон и маджента. Найдена через CP77",
                            checked = isCyberpunk,
                            onCheckedChange = viewModel::setCyberpunkTheme
                        ) else null,
                        if (ThemeOverlay.MATRIX in unlocked) ThemeRowData(
                            emoji = "💊",
                            title = "Матрица",
                            subtitle = "Зелёный терминал. Договор о читах в силе",
                            checked = isMatrix,
                            onCheckedChange = viewModel::setMatrixTheme
                        ) else null
                    )

                    for (row in questRows) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        ThemeRow(
                            emoji = row.emoji,
                            title = row.title,
                            subtitle = row.subtitle,
                            checked = row.checked,
                            onCheckedChange = row.onCheckedChange
                        )
                    }
                }
            }

            // Подсказка о квестах: список не выглядит сломанным, когда в нём
            // одна строка, и мотивирует искать остальные.
            Text(
                text = "Остальные темы появятся здесь, когда вы их найдёте. " +
                    "Потерянная тема исчезает из списка.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private class ThemeRowData(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit
)

@Composable
private fun ThemeRow(
    emoji: String,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(12.dp))
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
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
