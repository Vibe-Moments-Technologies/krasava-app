package com.jetbrains.kmpapp.screens.games

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Bolt
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
            GameCard(
                title = option.title,
                subtitle = "${option.width}×${option.height} · ${option.mines} мин",
                recordText = records[GamesViewModel.recordKey(option)]
                    ?.let { GamesViewModel.formatRecord(it) },
                icon = Icons.Filled.Bolt,
                onClick = { viewModel.openGame(option) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
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