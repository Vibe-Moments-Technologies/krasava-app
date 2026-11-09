package com.jetbrains.kmpapp.screens.components

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Включает/выключает обработку системного «назад» для всего поддерева.
 * Слой-родитель в [LayeredNavHost] полностью закрыт дочерним экраном, поэтому
 * его PlatformBackHandler не должен перехватывать системную кнопку — иначе
 * обработчик пустого родительского слоя (добавленный позже) выигрывает у
 * реального обработчика дочернего экрана (Android: LIFO).
 */
val LocalBackHandlerEnabled = staticCompositionLocalOf { true }
