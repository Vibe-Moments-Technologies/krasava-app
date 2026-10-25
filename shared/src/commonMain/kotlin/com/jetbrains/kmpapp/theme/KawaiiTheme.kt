package com.jetbrains.kmpapp.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Тема «Кавай»: аниме-интерфейс с холодной лавандово-мятной базой и точечным
 * розовым акцентом.
 *
 * Раньше она была бело-розовой и читалась как «Сакура номер два» — розовый
 * фон и розовые кнопки у обеих. Чтобы темы различались с первого взгляда,
 * роли разделены:
 * - Сакура остаётся тёплой розовой (персиковый подтон, розовые акценты);
 * - Кавай уходит в холодные пастельные тона: лаванда как primary, мята как
 *   secondary, розовый остался только точечным акцентом tertiary — как в
 *   аниме-интерфейсах, где акцентных цветов несколько, а не один.
 *
 * Тёмный вариант есть с самого начала: ночью светлая пастельная тема слепит,
 * а «кавай» не обязан быть светлым — неоновые пастельные акценты на глубоком
 * индиго читаются как тот же стиль, просто после отбоя.
 *
 * Даётся квестом: восемь быстрых taps по иконке раздела «Внешний вид»
 * (раньше этот квест выдавал Сакуру — она теперь доступна по умолчанию).
 */
val KawaiiLightColors = lightColorScheme(
    primary = Color(0xFF7A5CF0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4DCFF),
    onPrimaryContainer = Color(0xFF2C1A6B),
    secondary = Color(0xFF1FA5A0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCF5F1),
    onSecondaryContainer = Color(0xFF0A4341),
    tertiary = Color(0xFFFF5C93),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDCE8),
    onTertiaryContainer = Color(0xFF6E1436),
    background = Color(0xFFF8F6FF),
    onBackground = Color(0xFF2B2741),
    surface = Color(0xFFF8F6FF),
    onSurface = Color(0xFF2B2741),
    surfaceVariant = Color(0xFFEDE9FF),
    onSurfaceVariant = Color(0xFF615A85),
    surfaceContainer = Color(0xFFF1EDFF),
    surfaceContainerHigh = Color(0xFFE7E1FF),
    outline = Color(0xFFC3B8EC),
    outlineVariant = Color(0xFFDCD4F5),
    error = Color(0xFFD93A63),
    onError = Color.White,
    errorContainer = Color(0xFFFFDCE3),
    onErrorContainer = Color(0xFF5E0E24)
)

val KawaiiDarkColors = darkColorScheme(
    primary = Color(0xFFB9A6FF),
    onPrimary = Color(0xFF2A1470),
    primaryContainer = Color(0xFF432C9C),
    onPrimaryContainer = Color(0xFFE7DEFF),
    secondary = Color(0xFF5FE0D6),
    onSecondary = Color(0xFF00322F),
    secondaryContainer = Color(0xFF00504B),
    onSecondaryContainer = Color(0xFFB8FBF4),
    tertiary = Color(0xFFFF8FB5),
    onTertiary = Color(0xFF5E1330),
    tertiaryContainer = Color(0xFF8B2450),
    onTertiaryContainer = Color(0xFFFFD9E6),
    background = Color(0xFF120F20),
    onBackground = Color(0xFFE9E3FF),
    surface = Color(0xFF120F20),
    onSurface = Color(0xFFE9E3FF),
    surfaceVariant = Color(0xFF241E3A),
    onSurfaceVariant = Color(0xFFBDB3DF),
    surfaceContainer = Color(0xFF1B1730),
    surfaceContainerHigh = Color(0xFF271F42),
    outline = Color(0xFF6A5C9B),
    outlineVariant = Color(0xFF3A315C),
    error = Color(0xFFFF8FA8),
    onError = Color(0xFF4A0018),
    errorContainer = Color(0xFF7A1233),
    onErrorContainer = Color(0xFFFFD9E1)
)
