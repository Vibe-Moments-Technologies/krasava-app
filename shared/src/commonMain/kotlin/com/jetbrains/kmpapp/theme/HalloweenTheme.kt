package com.jetbrains.kmpapp.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * Активна ли сейчас хэллоуинская тема (автоматически 31 октября). Позволяет
 * любым экранам включать тематические иконки/эффекты, не зная о дате.
 */
val LocalHalloweenEnabled = compositionLocalOf { false }