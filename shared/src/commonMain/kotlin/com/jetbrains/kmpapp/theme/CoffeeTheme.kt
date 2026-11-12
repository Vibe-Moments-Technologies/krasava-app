package com.jetbrains.kmpapp.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

const val COFFEE_REQUIRED_REPOSITORIES = 4

val CoffeeLightColors = lightColorScheme(
    primary = Color(0xFF6F4A32),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF1DCC8),
    onPrimaryContainer = Color(0xFF2B170C),
    secondary = Color(0xFF8B5E3C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDDBF),
    onSecondaryContainer = Color(0xFF2E1504),
    tertiary = Color(0xFF9A6A3A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE2B8),
    onTertiaryContainer = Color(0xFF2F1B00),
    background = Color(0xFFFFF8F1),
    onBackground = Color(0xFF2A1C14),
    surface = Color(0xFFFFF8F1),
    onSurface = Color(0xFF2A1C14),
    surfaceVariant = Color(0xFFEDDCCD),
    onSurfaceVariant = Color(0xFF5F4B3E),
    surfaceContainer = Color(0xFFF8EDE3),
    surfaceContainerHigh = Color(0xFFF2E2D4),
    outline = Color(0xFF9C8170),
    outlineVariant = Color(0xFFD8C2B1),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val CoffeeDarkColors = darkColorScheme(
    primary = Color(0xFFE8C4A2),
    onPrimary = Color(0xFF422B1A),
    primaryContainer = Color(0xFF5C3E29),
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFFD7B08A),
    onSecondary = Color(0xFF3D2A19),
    secondaryContainer = Color(0xFF57402C),
    onSecondaryContainer = Color(0xFFF5DCC4),
    tertiary = Color(0xFFEBC17E),
    onTertiary = Color(0xFF412D00),
    tertiaryContainer = Color(0xFF5E4200),
    onTertiaryContainer = Color(0xFFFFE1A6),
    background = Color(0xFF1A120E),
    onBackground = Color(0xFFF3E3D7),
    surface = Color(0xFF1A120E),
    onSurface = Color(0xFFF3E3D7),
    surfaceVariant = Color(0xFF4E3A2D),
    onSurfaceVariant = Color(0xFFD6C1B1),
    surfaceContainer = Color(0xFF251A15),
    surfaceContainerHigh = Color(0xFF31231C),
    outline = Color(0xFF9F8574),
    outlineVariant = Color(0xFF524035),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

fun normalizeCoffeeRepositoryUrl(url: String): String? {
    val withoutScheme = url.trim()
        .removePrefix("https://")
        .removePrefix("http://")
        .lowercase()
    if (!withoutScheme.startsWith("github.com/")) return null
    val path = withoutScheme.removePrefix("github.com/").trim('/')
    if (path.isEmpty()) return null
    return "github.com/$path"
}

fun addCoffeeRepositoryVisit(visited: Set<String>, url: String): Set<String>? {
    val repository = normalizeCoffeeRepositoryUrl(url) ?: return null
    return visited + repository
}

fun isCoffeeQuestComplete(visited: Set<String>): Boolean =
    visited.size >= COFFEE_REQUIRED_REPOSITORIES
