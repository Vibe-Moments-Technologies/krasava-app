package com.jetbrains.kmpapp.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.data.model.DateUtils
import com.jetbrains.kmpapp.data.model.LessonType
import com.jetbrains.kmpapp.screens.schedule.DayLessonSummary
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.minus

/**
 * Полноценный месячный календарь: листание месяцев/лет, подсветка сегодня
 * и выбранного дня, цветные точки пар под числом (легенда внизу), «Сегодня».
 * Общий для расписания и свободных аудиторий.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthPickerDialog(
    initialDate: LocalDate,
    onDatePicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    lessonSummaries: Map<LocalDate, DayLessonSummary> = emptyMap()
) {
    val today = remember { DateUtils.today() }
    var displayedYear by remember { mutableStateOf(initialDate.year) }
    var displayedMonth by remember { mutableStateOf(initialDate.month.ordinal + 1) }
    // «Сегодня» отмечает текущий день, но НЕ выбирает его: выбор — только тап.
    var highlighted by remember { mutableStateOf(initialDate) }

    val daysInMonth = remember(displayedYear, displayedMonth) {
        val next = if (displayedMonth == 12) {
            LocalDate(displayedYear + 1, 1, 1)
        } else {
            LocalDate(displayedYear, displayedMonth + 1, 1)
        }
        next.minus(DatePeriod(days = 1)).dayOfMonth
    }
    val leadingEmptyDays = remember(displayedYear, displayedMonth) {
        LocalDate(displayedYear, displayedMonth, 1).dayOfWeek.ordinal // Пн = 0
    }
    val isDark = isSystemInDarkTheme()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        confirmButton = {
            TextButton(onClick = {
                displayedYear = today.year
                displayedMonth = today.month.ordinal + 1
                highlighted = today
            }) {
                Text("Сегодня", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (displayedMonth == 1) {
                            displayedMonth = 12
                            displayedYear -= 1
                        } else {
                            displayedMonth -= 1
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Предыдущий месяц"
                    )
                }

                Text(
                    text = "${DateUtils.formatMonthTitle(Month.entries[displayedMonth - 1])} $displayedYear",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = {
                        if (displayedMonth == 12) {
                            displayedMonth = 1
                            displayedYear += 1
                        } else {
                            displayedMonth += 1
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Следующий месяц"
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEachIndexed { idx, dayName ->
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (idx >= 5) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val totalRows = (leadingEmptyDays + daysInMonth + 6) / 7
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (row in 0 until totalRows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (col in 0 until 7) {
                                val dayNumber = row * 7 + col - leadingEmptyDays + 1
                                if (dayNumber !in 1..daysInMonth) {
                                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                } else {
                                    val cellDate = LocalDate(displayedYear, displayedMonth, dayNumber)
                                    val isSelected = cellDate == highlighted
                                    val isToday = cellDate == today
                                    val lessonTypes = lessonSummaries[cellDate]?.lessonTypes ?: emptyList()

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(1.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                    isToday -> Color.Transparent
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .then(
                                                if (isToday) {
                                                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                } else Modifier
                                            )
                                            .clickable {
                                                onDatePicked(cellDate)
                                                onDismiss()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = dayNumber.toString(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    col == 6 -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                            // До трёх точек — в ленточном календаре их больше
                                            // и сетка «плывёт»; легенда ниже расшифровывает.
                                            if (lessonTypes.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    lessonTypes.take(3).forEach { type ->
                                                        Box(
                                                            modifier = Modifier
                                                                .size(3.5.dp)
                                                                .clip(CircleShape)
                                                                .background(getLessonDotColor(type, isDark))
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Легенда типов пар (те же цвета, что в ленточном календаре).
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LessonType.entries.forEachIndexed { idx, type ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(getLessonDotColor(type, isDark))
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = type.displayName,
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (idx != LessonType.entries.lastIndex) {
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                    }
                }
            }
        }
    )
}

/** Цвета точек пар — единый источник для ленточного и месячного календарей. */
internal fun getLessonDotColor(type: LessonType, isDark: Boolean): Color = when (type) {
    LessonType.LECTURE -> if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    LessonType.PRACTICE -> if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
    LessonType.LAB -> if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C)
    LessonType.OTHER -> if (isDark) Color(0xFFC084FC) else Color(0xFF9333EA)
}
