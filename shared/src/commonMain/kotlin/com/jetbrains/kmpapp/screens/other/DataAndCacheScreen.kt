package com.jetbrains.kmpapp.screens.other
import com.jetbrains.kmpapp.theme.GlitchText

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.data.model.StorageCategory
import com.jetbrains.kmpapp.data.model.StorageStats
import com.jetbrains.kmpapp.screens.components.PlatformBackHandler
import com.jetbrains.kmpapp.screens.map.MapRepository

/**
 * «Данные и память» — реальный разбор хранилища.
 *
 * Раньше экран показывал цифры, зашитые в вёрстку: карты «~2.2 МБ» (на деле
 * 3.17 МБ), задачи «~64 КБ» (независимо от содержимого), а «Настройки»
 * считались по трём ключам из восьмидесяти. Теперь каждая запись хранилища
 * относится к категории и считается по фактическим байтам UTF-8, а размер
 * карт читается из самих ресурсов.
 */
@Composable
fun DataAndCacheScreen(
    viewModel: OtherViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlatformBackHandler(onBack = onBack)

    val storageStats by viewModel.storageStats.collectAsState()
    val notesStorage: com.jetbrains.kmpapp.data.storage.LessonNotesStorage =
        org.koin.compose.koinInject()
    val notes by notesStorage.notes.collectAsState()

    // Реальный объём SVG-карт: читается из ресурсов, а не константа.
    var mapsBytes by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        viewModel.refreshStorageStats()
        mapsBytes = runCatching { MapRepository.mapsSizeBytes() }.getOrDefault(0L)
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
                    text = "Данные и память",
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Сводка: реальный объём данных + карт ──────────────
            val dataBytes = storageStats.totalSizeBytes
            val grandTotal = dataBytes + mapsBytes

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            GlitchText(
                                text = "Занято на устройстве",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            GlitchText(
                                text = StorageStats.formatBytes(grandTotal),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            GlitchText(
                                text = "данные ${StorageStats.formatBytes(dataBytes)} · карты ${StorageStats.formatBytes(mapsBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    StorageBar(stats = storageStats, mapsBytes = mapsBytes)
                    Spacer(modifier = Modifier.height(14.dp))
                    StorageLegend(stats = storageStats, mapsBytes = mapsBytes)
                }
            }

            // ── Детализация по фактическим категориям ─────────────
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    GlitchText(
                        text = "Что именно занимает место",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    GlitchText(
                        text = "Размеры посчитаны по фактическому содержимому хранилища.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val rows = storageStats.categories.filter { it.sizeBytes > 0L }
                    if (rows.isEmpty()) {
                        GlitchText(
                            text = "Хранилище пусто — данные ещё не загружались.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    rows.forEachIndexed { index, usage ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
                        }
                        StorageDetailRow(
                            icon = usage.category.icon(),
                            color = usage.category.color(),
                            title = usage.category.title,
                            subtitle = usage.category.subtitle(storageStats, usage.entriesCount),
                            size = StorageStats.formatBytes(usage.sizeBytes)
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                    StorageDetailRow(
                        icon = Icons.Default.Map,
                        color = MAPS_COLOR,
                        title = "Векторные карты корпусов",
                        subtitle = "SVG всех этажей, входят в приложение",
                        size = StorageStats.formatBytes(mapsBytes)
                    )
                }
            }

            // ── Очистка старых заметок ────────────────────────────
            if (notes.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        GlitchText(
                            text = "Заметки к прошедшим парам",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        GlitchText(
                            text = "Сейчас ${notes.size} заметок на " +
                                StorageStats.formatBytes(notesStorage.getStorageSizeBytes()) +
                                ". Привязанные к занятиям старше 90 дней можно удалить.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                val cutoff = kotlin.time.Clock.System.now().toEpochMilliseconds() -
                                    90L * 24 * 60 * 60 * 1000
                                notesStorage.cleanOldLessonNotes(cutoff)
                                viewModel.refreshStorageStats()
                            }
                        ) {
                            GlitchText("Очистить заметки к прошедшим занятиям")
                        }
                    }
                }
            }

            // ── Почему нет очистки кэша ───────────────────────────
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        GlitchText(
                            text = "Почему нет очистки кэша?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    GlitchText(
                        text = "Приложение спроектировано по концепции Offline-First: расписания, карты корпусов и задачи сохраняются локально и работают мгновенно даже без сети.\n\n" +
                            "В отличие от веб-браузеров, кэш не накапливается сотнями мегабайт, а точечно и автоматически обновляется в фоне при наличии интернета. " +
                            "Поэтому приложение занимает минимум памяти и не требует ручной очистки.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/** Цвет карт в диаграмме — фиксированный, как у остальных категорий. */
private val MAPS_COLOR = Color(0xFFF59E0B)

/** Цвет категории: стабильный, чтобы диаграмма не «прыгала» между запусками. */
@Composable
private fun StorageCategory.color(): Color = when (this) {
    StorageCategory.SCHEDULES -> MaterialTheme.colorScheme.primary
    StorageCategory.TARGETS -> Color(0xFF0284C7)
    StorageCategory.USER_CONTENT -> Color(0xFF8B5CF6)
    StorageCategory.TASKS -> Color(0xFFEC4899)
    StorageCategory.SETTINGS -> Color(0xFF10B981)
    StorageCategory.GAMES -> Color(0xFFEF4444)
    StorageCategory.SERVICE -> Color(0xFF64748B)
}

private fun StorageCategory.icon(): ImageVector = when (this) {
    StorageCategory.SCHEDULES -> Icons.Default.Folder
    StorageCategory.TARGETS -> Icons.Default.PieChart
    StorageCategory.USER_CONTENT -> Icons.Default.EditNote
    StorageCategory.TASKS -> Icons.Default.TaskAlt
    StorageCategory.SETTINGS -> Icons.Default.Tune
    StorageCategory.GAMES -> Icons.Default.SportsEsports
    StorageCategory.SERVICE -> Icons.Default.Settings
}

/** Подпись категории с фактическими счётчиками вместо общих слов. */
private fun StorageCategory.subtitle(stats: StorageStats, entries: Int): String = when (this) {
    StorageCategory.SCHEDULES ->
        "${stats.schedulesCount} расписаний · ${stats.lessonsCount} занятий"
    StorageCategory.TARGETS -> "${stats.targetsCount} групп / преподавателей"
    StorageCategory.USER_CONTENT -> "свои пары, скрытые, конспекты · $entries записей"
    StorageCategory.TASKS -> "предметы и задачи · $entries записей"
    StorageCategory.SETTINGS -> "тема, отображение, уведомления · $entries записей"
    StorageCategory.GAMES -> "рекорды и настройки · $entries записей"
    StorageCategory.SERVICE -> "кэш аватарок, конфиг, счётчики · $entries записей"
}

/** Полоса долей: ширина каждого сегмента = реальный объём категории. */
@Composable
private fun StorageBar(stats: StorageStats, mapsBytes: Long) {
    val total = (stats.totalSizeBytes + mapsBytes).coerceAtLeast(1L).toFloat()
    val emptyColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val segments = stats.categories.filter { it.sizeBytes > 0L }
        .map { it.category.color() to (it.sizeBytes / total) } +
        listOf(MAPS_COLOR to (mapsBytes / total))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(emptyColor)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            segments.forEach { (color, ratio) ->
                if (ratio > 0f) {
                    val animated by animateFloatAsState(ratio.coerceIn(0f, 1f))
                    Box(
                        modifier = Modifier
                            .weight(animated.coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .background(color)
                    )
                }
            }
        }
    }
}

/** Легенда: четыре самые крупные категории, чтобы не ломать вёрстку. */
@Composable
private fun StorageLegend(stats: StorageStats, mapsBytes: Long) {
    val top = stats.categories.filter { it.sizeBytes > 0L }.take(3)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        top.forEach { usage ->
            StorageLegendItem(
                color = usage.category.color(),
                label = usage.category.title,
                value = StorageStats.formatBytes(usage.sizeBytes),
                modifier = Modifier.weight(1f)
            )
        }
        if (mapsBytes > 0L) {
            StorageLegendItem(
                color = MAPS_COLOR,
                label = "Карты",
                value = StorageStats.formatBytes(mapsBytes),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StorageLegendItem(
    color: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(6.dp))
            GlitchText(
                text = label,
                fontSize = 11.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        GlitchText(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StorageDetailRow(
    icon: ImageVector,
    color: Color,
    title: String,
    subtitle: String,
    size: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                GlitchText(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                GlitchText(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        GlitchText(
            text = size,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}