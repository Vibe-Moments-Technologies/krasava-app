package com.jetbrains.kmpapp.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Тема «Кавай»: полностью анимешная, милая, в бело-розовых тонах.
 *
 * В отличие от Сакуры (пастельно-розовая, есть тёмный вариант) Кавай —
 * светлая моно-палитра: белый фон с розовыми акцентами, как в классических
 * аниме-интерфейсах. Тёмного варианта нет сознательно: бело-розовая
 * концепция в тёмных тонах перестаёт быть собой (по тому же принципу,
 * по которому Хэллоуин существует только в тёмном варианте).
 *
 * Даётся квестом: восемь быстрых taps по иконке раздела «Внешний вид»
 * (раньше этот квест выдавал Сакуру — она теперь доступна по умолчанию).
 */
val KawaiiColors = lightColorScheme(
    primary = Color(0xFFFF6FA5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD6E7),
    onPrimaryContainer = Color(0xFF7A3B57),
    secondary = Color(0xFFFF9EC4),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE6F0),
    onSecondaryContainer = Color(0xFF8C5068),
    tertiary = Color(0xFFB78CE0),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEBDFFA),
    onTertiaryContainer = Color(0xFF5B3E7E),
    background = Color(0xFFFFFBFD),
    onBackground = Color(0xFF6D4C58),
    surface = Color(0xFFFFFBFD),
    onSurface = Color(0xFF6D4C58),
    surfaceVariant = Color(0xFFFFF0F6),
    onSurfaceVariant = Color(0xFFA07786),
    surfaceContainer = Color(0xFFFFF3F8),
    surfaceContainerHigh = Color(0xFFFFE6F0),
    outline = Color(0xFFF0C6D8),
    outlineVariant = Color(0xFFFADFEA),
    error = Color(0xFFE06690),
    onError = Color.White,
    errorContainer = Color(0xFFFFDCE6),
    onErrorContainer = Color(0xFF7A2B45)
)
