package com.jetbrains.kmpapp.screens.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.screens.components.LayeredNavHost
import com.jetbrains.kmpapp.screens.components.PlatformBackHandler

/**
 * Раздел «Игры»: меню с выбором игры. Сейчас в меню ровно одна игра — «Сапер»,
 * у неё несколько вариантов поля, каждый со своим рекордом. Сама игра
 * открывается послойно поверх меню, назад — свайп, системная кнопка или стрелка.
 */
@Composable
fun GamesScreen(
    viewModel: GamesViewModel,
    modifier: Modifier = Modifier
) {
    val activeGame by viewModel.activeGame.collectAsState()

    LayeredNavHost(
        screen = activeGame,
        parentScreen = null,
        onBackToParent = { viewModel.closeGame() },
        initiallyRevealed = remember { activeGame != null },
        swipeGestureEnabled = { true },
        rootContent = {
            GamesMenu(viewModel = viewModel, modifier = modifier)
        },
        screenContent = { game, back ->
            when (game) {
                GamesViewModel.Game.MINESWEEPER -> {
                    PlatformBackHandler(onBack = back)
                    MinesweeperScreen(viewModel = viewModel, onBack = back)
                }
                null -> {}
            }
        },
        modifier = modifier
    )
}

@Composable
private fun GamesMenu(
    viewModel: GamesViewModel,
    modifier: Modifier = Modifier
) {
    val records by viewModel.records.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            // Свободное место под плавающую панель страниц.
            .padding(bottom = 120.dp)
    ) {
        Text(
            text = "Игры",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Сапер — варианты поля от детского сада до пенсионера, " +
                "у каждого свой рекорд.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        MinesweeperDifficulty.entries.forEach { option ->
            val record = records[GamesViewModel.recordKey(option)]
            if (record?.seconds == 0) {
                // Вариант «решён» с первого нажатия: режим сломан, вместо
                // карточки — кнопка ERROR, включающая тему сбоя.
                ErrorGameCard(
                    subtitle = "${option.width}×${option.height} · ${option.mines} мин",
                    onClick = { viewModel.openGame(option, errorTheme = true) }
                )
            } else {
                GameCard(
                    title = option.title,
                    subtitle = "${option.width}×${option.height} · ${option.mines} мин",
                    recordText = record?.let { GamesViewModel.formatRecord(it) },
                    icon = Icons.Filled.Bolt,
                    onClick = { viewModel.openGame(option) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ErrorGameCard(
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A0B0B)),
        border = BorderStroke(1.dp, Color(0xFF7A1A1A)),
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF4A1010)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Report,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                GlitchTitle(
                    text = "ERROR",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE57373)
                )
                Text(
                    text = "Рекорд: 0 с — сбой. Нажми, чтобы активировать тему",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFF8A80)
                )
            }
        }
    }
}

/** Заголовок с эффектом глюка: красная надпись с цветными «двойниками». */
@Composable
private fun GlitchTitle(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    val base = style.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Black,
        letterSpacing = 3.sp
    )
    Box(modifier = modifier) {
        Text(
            text = text,
            style = base,
            color = Color(0x4DFFB400),
            modifier = Modifier.offset(2.dp, 2.dp)
        )
        Text(
            text = text,
            style = base,
            color = Color(0x6600E5FF),
            modifier = Modifier.offset((-2).dp, (-1).dp)
        )
        Text(
            text = text,
            style = base,
            color = Color(0xFFFF3B30)
        )
    }
}

@Composable
private fun GameCard(
    title: String,
    subtitle: String,
    recordText: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = recordText?.let { "Рекорд: $it" } ?: "Рекорд пока не установлен",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (recordText != null) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                    color = if (recordText != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}