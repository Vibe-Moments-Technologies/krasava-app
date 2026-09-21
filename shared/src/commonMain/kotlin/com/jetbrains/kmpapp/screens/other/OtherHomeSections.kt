package com.jetbrains.kmpapp.screens.other

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.data.model.AppVersion
import com.jetbrains.kmpapp.data.update.UpdateCheckResult
import com.jetbrains.kmpapp.data.update.UpdateUrgency
import com.jetbrains.kmpapp.screens.components.AppTab

/** Концентратор вкладок, спрятанных из дока (виден только если такие есть). */
@Composable
internal fun HiddenTabsCard(
    hiddenTabs: List<AppTab>,
    onNavigateToTab: (AppTab) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Горизонтальная галерея без заголовка: пилюли самоочевидны
            // (иконка + название), а заголовок съедал вертикальный бюджет
            // главной «Другого», когда блок виден.
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(hiddenTabs) { tab ->
                    Surface(
                        onClick = { onNavigateToTab(tab) },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = tab.selectedIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Карточка версии/обновления — информационная. Никаких переходов и
 * установщиков: приложение обновляется маркетом, sideload — вручную.
 * Тап при актуальной версии — повторная проверка.
 */
@Composable
internal fun UpdateStatusCard(
    updateResult: UpdateCheckResult?,
    isCheckingUpdate: Boolean,
    onCheckForUpdates: () -> Unit
) {
    val urgency = updateResult?.urgency ?: UpdateUrgency.UP_TO_DATE
    val hasUpdate = updateResult?.hasUpdate == true

    val cardContainerColor = when (urgency) {
        UpdateUrgency.CRITICAL -> Color(0xFF581C87).copy(alpha = 0.20f)
        UpdateUrgency.NEW_VERSION -> Color(0xFFDC2626).copy(alpha = 0.16f)
        UpdateUrgency.MINOR_BUILD -> Color(0xFFD97706).copy(alpha = 0.16f)
        UpdateUrgency.UP_TO_DATE -> MaterialTheme.colorScheme.surfaceContainer
    }

    val iconBoxColor = when (urgency) {
        UpdateUrgency.CRITICAL -> Color(0xFF581C87).copy(alpha = 0.40f)
        UpdateUrgency.NEW_VERSION -> Color(0xFFDC2626).copy(alpha = 0.30f)
        UpdateUrgency.MINOR_BUILD -> Color(0xFFD97706).copy(alpha = 0.30f)
        UpdateUrgency.UP_TO_DATE -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val accentTint = when (urgency) {
        UpdateUrgency.CRITICAL -> Color(0xFFC084FC)
        UpdateUrgency.NEW_VERSION -> Color(0xFFEF4444)
        UpdateUrgency.MINOR_BUILD -> Color(0xFFF59E0B)
        UpdateUrgency.UP_TO_DATE -> if (isCheckingUpdate) MaterialTheme.colorScheme.primary else Color(0xFF22C55E)
    }

    val titleText = when {
        isCheckingUpdate -> "Проверка обновлений..."
        urgency == UpdateUrgency.CRITICAL -> "Важное обновление!"
        urgency == UpdateUrgency.NEW_VERSION -> "Вышла новая версия!"
        urgency == UpdateUrgency.MINOR_BUILD -> "Доступна новая сборка"
        else -> "У вас актуальная версия"
    }

    val subtitleText = when {
        urgency == UpdateUrgency.CRITICAL ->
            "Версия ${updateResult?.latestVersion} • Важные исправления безопасности"
        hasUpdate ->
            "Версия ${updateResult?.latestVersion} • Обновитесь в магазине приложений"
        else -> AppVersion.DISPLAY_VERSION
    }

    val statusIcon = when {
        isCheckingUpdate -> Icons.Default.Refresh
        urgency == UpdateUrgency.CRITICAL -> Icons.Default.SystemUpdate
        urgency == UpdateUrgency.NEW_VERSION -> Icons.Default.SystemUpdate
        urgency == UpdateUrgency.MINOR_BUILD -> Icons.Default.Refresh
        else -> Icons.Default.CheckCircle
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            // Информационная карточка: кликабельна только для ручной
            // повторной проверки, когда обновление не найдено.
            .clickable(enabled = !hasUpdate && !isCheckingUpdate) { onCheckForUpdates() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBoxColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = accentTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasUpdate) accentTint else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


