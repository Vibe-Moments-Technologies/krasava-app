package com.jetbrains.kmpapp.theme

import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/**
 * «Тема Error»: тёмно-красный сбой, включается для варианта, решённого
 * с одного нажатия (рекорд 0 с). В отличие от Сакуры/Матрицы/Киберпанка
 * это не выбор пользователя, а «сломанное» состояние приложения — поэтому
 * глобальная палитра и моноширинная типографика приходят издалека, как
 * оверлей поверх любой другой темы.
 */
val ErrorColors = darkColorScheme(
    primary = Color(0xFFFF5252),
    onPrimary = Color(0xFF2A0000),
    primaryContainer = Color(0xFF7A0E0E),
    onPrimaryContainer = Color(0xFFFFDAD4),
    secondary = Color(0xFFB4554F),
    onSecondary = Color(0xFF2A0B0B),
    secondaryContainer = Color(0xFF5A1A1A),
    onSecondaryContainer = Color(0xFFFFDAD4),
    background = Color(0xFF150505),
    onBackground = Color(0xFFFFE5E2),
    surface = Color(0xFF150505),
    onSurface = Color(0xFFFFE5E2),
    surfaceContainer = Color(0xFF2A0B0B),
    surfaceContainerHigh = Color(0xFF3A1010),
    onSurfaceVariant = Color(0xFFE57373),
    error = Color(0xFFFF3B30),
    errorContainer = Color(0xFF8B0000),
    onErrorContainer = Color(0xFFFFB4AB),
    outline = Color(0xFF7A3A38),
    outlineVariant = Color(0xFF5A2626),
    inverseSurface = Color(0xFFFFE5E2),
    inverseOnSurface = Color(0xFF3A0A0A)
)

/** Та же типографика, что и по умолчанию, но моноширинная — как в «Сапере». */
val ErrorTypography: Typography = Typography().run {
    val family = FontFamily.Monospace
    Typography(
        displayLarge = displayLarge.copy(fontFamily = family),
        displayMedium = displayMedium.copy(fontFamily = family),
        displaySmall = displaySmall.copy(fontFamily = family),
        headlineLarge = headlineLarge.copy(fontFamily = family),
        headlineMedium = headlineMedium.copy(fontFamily = family),
        headlineSmall = headlineSmall.copy(fontFamily = family),
        titleLarge = titleLarge.copy(fontFamily = family),
        titleMedium = titleMedium.copy(fontFamily = family),
        titleSmall = titleSmall.copy(fontFamily = family),
        bodyLarge = bodyLarge.copy(fontFamily = family),
        bodyMedium = bodyMedium.copy(fontFamily = family),
        bodySmall = bodySmall.copy(fontFamily = family),
        labelLarge = labelLarge.copy(fontFamily = family),
        labelMedium = labelMedium.copy(fontFamily = family),
        labelSmall = labelSmall.copy(fontFamily = family)
    )
}