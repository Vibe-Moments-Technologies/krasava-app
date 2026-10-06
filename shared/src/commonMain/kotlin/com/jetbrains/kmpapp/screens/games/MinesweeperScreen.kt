package com.jetbrains.kmpapp.screens.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

/**
 * Игра «Сапер». Поле рисуется сеткой клеток: тап открывает клетку,
 * долгое нажатие ставит флаг, двойной тап по числу открывает остаток
 * области вокруг него, если вокруг выставлены все флаги.
 */
@Composable
fun MinesweeperScreen(
    viewModel: GamesViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val board by viewModel.board.collectAsState()
    val status by viewModel.status.collectAsState()
    val elapsed by viewModel.elapsedSeconds.collectAsState()
    val flagMode by viewModel.flagMode.collectAsState()
    val difficulty by viewModel.difficulty.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 12.dp)
            // Свободное место под плавающую панель страниц.
            .padding(bottom = 110.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "В меню игр"
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Сапер",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${difficulty.width}×${difficulty.height} · ${difficulty.mines} мин",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { viewModel.restart() }) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Новая партия"
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = "Мины",
                value = (board?.remainingMines ?: difficulty.mines).toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Время",
                value = formatSeconds(elapsed),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            MinesweeperGrid(
                width = difficulty.width,
                height = difficulty.height,
                board = board,
                onCellClick = { index ->
                    viewModel.onCellClick(index)
                },
                onCellLongClick = { index ->
                    viewModel.onCellLongClick(index)
                },
                onCellDoubleClick = { index ->
                    viewModel.onCellDoubleClick(index)
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (status) {
            MinesweeperStatus.WON -> ResultCard(
                title = "Победа!",
                subtitle = "Все мины найдены за ${formatSeconds(elapsed)}.",
                primaryLabel = "Ещё партия",
                onPrimary = { viewModel.restart() },
                onMenu = onBack
            )
            MinesweeperStatus.LOST -> ResultCard(
                title = "Мина!",
                subtitle = "Раскрыто за ${formatSeconds(elapsed)}. Попробуй ещё раз.",
                primaryLabel = "Ещё партия",
                onPrimary = { viewModel.restart() },
                onMenu = onBack
            )
            else -> Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilterChip(
                    selected = flagMode,
                    onClick = { viewModel.toggleFlagMode() },
                    label = { Text(if (flagMode) "Режим флага" else "Обычный режим") }
                )
                Text(
                    text = "Долгое нажатие — флаг",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ResultCard(
    title: String,
    subtitle: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onMenu: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPrimary) { Text(primaryLabel) }
                Button(onClick = onMenu) { Text("В меню") }
            }
        }
    }
}

/** Поле: клетки одинаковые, размер подбирается под свободное место. */
private val MinCellSize = 20.dp

/**
 * Поле рисуется сеткой клеток одинакового размера. Размер клетки — это то,
 * сколько места останется под поле после шапки и панелей. На больших
 * вариантах (30×16 и больше) клетка упёрлась бы в непригодный для тапа
 * размер, поэтому снизу стоит минимум: если поле не помещается — клетки
 * берут минимальный размер, а само поле прокручивается в обе стороны.
 */
@Composable
private fun MinesweeperGrid(
    width: Int,
    height: Int,
    board: MinesweeperBoard?,
    onCellClick: (Int) -> Unit,
    onCellLongClick: (Int) -> Unit,
    onCellDoubleClick: (Int) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Если ограничение по высоте бесконечно (поле ещё не измерено) —
        // считаем клетку по ширине, иначе получится нулевой размер.
        val byWidth = maxWidth / width.coerceAtLeast(1)
        val byHeight = maxHeight / height.coerceAtLeast(1)
        val fitted = if (byHeight.value.isFinite()) minOf(byWidth, byHeight) else byWidth
        val cell = maxOf(fitted, MinCellSize)
        val needsScroll = cell * width > maxWidth || cell * height > maxHeight
        val horizontalState = rememberScrollState()
        val verticalState = rememberScrollState()
        val contentModifier = Modifier
            .size(cell * width, cell * height)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .let { if (needsScroll) it.horizontalScroll(horizontalState) else it }
                .let { if (needsScroll) it.verticalScroll(verticalState) else it },
            // При прокрутке выравнивание по центру спрятало бы начало поля,
            // до которого невозможно доскроллить.
            contentAlignment = if (needsScroll) Alignment.TopStart else Alignment.Center
        ) {
            Column(modifier = contentModifier) {
                for (y in 0 until height) {
                    Row {
                        for (x in 0 until width) {
                            val index = y * width + x
                            val cellState = board?.cells?.getOrNull(index)
                            MinesweeperCellView(
                                state = cellState,
                                size = cell,
                                modifier = Modifier.combinedClickable(
                                    onClick = { onCellClick(index) },
                                    onLongClick = { onCellLongClick(index) },
                                    onDoubleClick = { onCellDoubleClick(index) }
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MinesweeperCellView(
    state: MinesweeperCell?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val revealed = state?.isRevealed == true
    val isMine = state?.isMine == true
    val flagged = state?.isFlagged == true
    // Шрифт масштабируется от клетки: в 64×48 клетка меньше строки
    // типографики, и фиксированный размер ломал бы вёрстку.
    val glyphStyle = MaterialTheme.typography.titleMedium.copy(
        fontSize = minOf(size.value * 0.5f, 24f).sp
    )

    Box(
        modifier = modifier
            .size(size)
            .background(
                when {
                    revealed && isMine -> scheme.errorContainer
                    revealed -> scheme.surface
                    else -> scheme.surfaceContainerHigh
                }
            )
            .border(0.5.dp, scheme.outlineVariant.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        when {
            revealed && isMine -> {
                Text(text = "✖", color = scheme.onErrorContainer, style = glyphStyle)
            }
            revealed && flagged -> {
                // Флаг стоял на чистой клетке — ошибка игрока, показываем при проигрыше.
                Text(text = "⚑", color = scheme.error, style = glyphStyle)
            }
            revealed && (state?.adjacentMines ?: 0) > 0 -> {
                Text(
                    text = state!!.adjacentMines.toString(),
                    color = numberColor(state.adjacentMines),
                    style = glyphStyle,
                    fontWeight = FontWeight.Bold
                )
            }
            flagged -> {
                Box(
                    modifier = Modifier
                        .size(size * 0.4f)
                        .clip(CircleShape)
                        .background(scheme.primary)
                )
            }
        }
    }
}

/** Классические цвета цифр: 1 — синий, 2 — зелёный и так далее. */
@Composable
private fun numberColor(value: Int): Color = when (value) {
    1 -> Color(0xFF1976D2)
    2 -> Color(0xFF388E3C)
    3 -> Color(0xFFD32F2F)
    4 -> Color(0xFF512DA8)
    5 -> Color(0xFF8D6E63)
    6 -> Color(0xFF0097A7)
    7 -> Color(0xFF303F9F)
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun formatSeconds(total: Int): String {
    val minutes = total / 60
    val seconds = total % 60
    return if (minutes > 0) {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    } else {
        seconds.toString()
    }
}