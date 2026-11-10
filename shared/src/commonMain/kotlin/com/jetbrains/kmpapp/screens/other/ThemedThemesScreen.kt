package com.jetbrains.kmpapp.screens.other

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.screens.components.PlatformBackHandler
import com.jetbrains.kmpapp.theme.CyberpunkVariant
import com.jetbrains.kmpapp.theme.GlitchText
import com.jetbrains.kmpapp.theme.ThemeOverlay

/**
 * Подстраница «Тематические темы»: тумблеры всех тем-надстроек.
 *
 * Состав списка:
 * - Сакура — всегда: розовая тема доступна по умолчанию и включена сразу;
 * - Кавай, Киберпанк, Матрица — видны всегда. Пока тема не найдена, строка
 *   заблокирована: нажатие показывает подсказку, но не включает её;
 * - Хэллоуин — строка появляется в сезон (28 октября — 3 ноября) и остаётся
 *   навсегда после первого включения: вручную из списка, в сезон или через
 *   «Отладку и эксперименты». Так вне сезона её можно включить повторно.
 *
 * Отозванная тема (например, Матрица за нарушение правил) остаётся в списке,
 * но показывает отдельное сообщение и не может быть включена.
 *
 * У «Киберпанка» под строкой появляется выборщик палитры (новая/старая) — но
 * только пока тема найдена и включена. Выбор хранится отдельно от оверлея,
 * поэтому переключение на другую тему его не сбрасывает.
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
    val isHalloween by viewModel.isHalloweenActive.collectAsState()
    val halloweenVisible by viewModel.isHalloweenRowVisible.collectAsState()
    val cyberpunkVariant by viewModel.cyberpunkVariant.collectAsState()
    val unlocked by viewModel.unlockedThemes.collectAsState()
    val revoked by viewModel.revokedThemes.collectAsState()
    var lockDialog by remember { mutableStateOf<ThemeLockDialog?>(null) }

    // Сезонная тема перекрывает оформление, но не стирает выбранную базу:
    // после 3 ноября Сакура вернётся сама. Поэтому в списке «включённой»
    // считается только та тема, которая реально рисует приложение — иначе
    // в сезон два тумблера выглядели бы включёнными одновременно.
    fun acting(selected: Boolean): Boolean = selected && !isHalloween

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
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                    ThemeRow(
                        title = "Сакура",
                        checked = acting(isSakura),
                        onCheckedChange = viewModel::setSakuraTheme
                    )

                    // Квестовые темы видны всегда: ненайденная строка не
                    // активируется, а показывает подсказку. Порядок
                    // фиксированный, чтобы список не прыгал при разблокировках.
                    val questRows = listOf(
                        ThemeRowData(
                            title = "Кавай",
                            overlay = ThemeOverlay.KAWAII,
                            checked = acting(isKawaii),
                            onCheckedChange = viewModel::setKawaiiTheme
                        ),
                        ThemeRowData(
                            title = "Киберпанк",
                            overlay = ThemeOverlay.CYBERPUNK,
                            checked = acting(isCyberpunk),
                            onCheckedChange = viewModel::setCyberpunkTheme,
                            hasVariantPicker = true
                        ),
                        ThemeRowData(
                            title = "Матрица",
                            overlay = ThemeOverlay.MATRIX,
                            checked = acting(isMatrix),
                            onCheckedChange = viewModel::setMatrixTheme
                        )
                    )

                    for (row in questRows) {
                        val locked = row.overlay !in unlocked || row.overlay in revoked
                        ThemeRowDivider()
                        ThemeRow(
                            title = row.title,
                            checked = !locked && row.checked,
                            onCheckedChange = row.onCheckedChange,
                            locked = locked,
                            onLockedClick = {
                                val isRevoked = row.overlay in revoked
                                lockDialog = ThemeLockDialog(
                                    title = if (isRevoked) "Тема недоступна" else "Тема не найдена",
                                    message = if (isRevoked) THEME_REVOKED_MESSAGE
                                    else THEME_NOT_FOUND_MESSAGE
                                )
                            }
                        )
                        // Вариант палитры Киберпанка виден, только пока тема
                        // найдена и действительно включена: настраивать то,
                        // что не рисуется, незачем. Выбор хранится отдельно
                        // от оверлея, поэтому выключение темы его не теряет.
                        if (!locked && row.hasVariantPicker && row.checked) {
                            CyberpunkVariantPicker(
                                selected = cyberpunkVariant,
                                onSelect = viewModel::setCyberpunkVariant
                            )
                        }
                    }

                    // Сезонная строка живёт по правилу темы, а не по
                    // разблокировке: пока сезон идёт, её видно и можно гасить;
                    // сезон кончился — строка исчезает вместе с темой.
                    if (halloweenVisible) {
                        ThemeRowDivider()
                        ThemeRow(
                            title = "Хэллоуин",
                            checked = isHalloween,
                            onCheckedChange = viewModel::setHalloweenTheme
                        )
                    }
                }
            }

            // Подсказка о квестах: заблокированные строки видны заранее
            // и мотивируют искать остальные темы.
            Text(
                text = "Недоступные темы видны заранее, но включатся только " +
                    "после того, как вы их найдёте.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    lockDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = { lockDialog = null },
            title = { GlitchText(dialog.title) },
            text = { GlitchText(dialog.message) },
            confirmButton = {
                TextButton(onClick = { lockDialog = null }) {
                    GlitchText("Понятно")
                }
            }
        )
    }
}

private const val THEME_NOT_FOUND_MESSAGE = "Вы еще не нашли тему"
private const val THEME_REVOKED_MESSAGE =
    "Вам нельзя активировать эту тему за нарушение правил"

private data class ThemeLockDialog(
    val title: String,
    val message: String
)

private class ThemeRowData(
    val title: String,
    val overlay: ThemeOverlay,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
    /** Есть ли у темы выборщик варианта палитры (пока — только Киберпанк). */
    val hasVariantPicker: Boolean = false
)

@Composable
private fun ThemeRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

/**
 * Строка темы: название слева, тумблер или замок справа.
 *
 * Заблокированная строка остаётся кликабельной, чтобы показать подсказку,
 * но не переключает тему. Название занимает `weight(1f)` в том же Row, что
 * и элемент справа, — иначе длинные подписи выталкивали тумблер за экран.
 */
@Composable
private fun ThemeRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    locked: Boolean = false,
    onLockedClick: (() -> Unit)? = null
) {
    val clickModifier = if (locked && onLockedClick != null) {
        Modifier.clickable(onClick = onLockedClick)
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(clickModifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlitchText(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (locked) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        if (locked) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Тема недоступна",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

/**
 * Выбор палитры «Киберпанка»: новая (жёлтый/циан) или старая (маджента/циан).
 *
 * Показан только когда тема включена (см. вызов). Выбор живёт в хранилище
 * отдельно от оверлея, поэтому он сохраняется при переключении на другую тему
 * и обратно — как и просил владелец.
 */
@Composable
private fun CyberpunkVariantPicker(
    selected: CyberpunkVariant,
    onSelect: (CyberpunkVariant) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 10.dp)) {
        GlitchText(
            text = "Палитра",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CyberpunkVariant.entries.forEach { variant ->
                FilterChip(
                    selected = variant == selected,
                    onClick = { onSelect(variant) },
                    label = { Text(variant.displayName) }
                )
            }
        }
    }
}
