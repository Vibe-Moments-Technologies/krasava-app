package com.jetbrains.kmpapp.screens.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Свайп «назад» от левого края, как системный iOS:
 *  - экран едет за пальцем 1:1;
 *  - короткий БЫСТРЫЙ флик — сразу «назад», даже если проехал мало;
 *  - медленный малый сдвиг — отскакивает на место;
 *  - дотянул на четверть экрана — «назад» в моменте, родитель
 *    появляется под пальцем.
 * Плавность появления родителя задаётся в transitionSpec хостов
 * (LinearOutSlowIn 350 мс — продолжение движения пальца, не «хлопок»).
 * Один модификатор — все подстраницы приложения.
 */
fun Modifier.swipeToDismissBack(
    enabled: Boolean = true,
    edgeWidthPx: Float = 200f,
    thresholdPx: Float = 120f,
    requireEdge: Boolean = true,
    onBack: () -> Unit
): Modifier = composed {
    if (!enabled) return@composed this

    var dragPx by remember { mutableFloatStateOf(0f) }
    var startedAtEdge by remember { mutableStateOf(false) }
    var committed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val velocityTracker = remember { VelocityTracker() }
    val flingVelocityPx = with(LocalDensity.current) { 800.dp.toPx() }

    fun tryCommit() {
        if (!committed) {
            committed = true
            onBack()
        }
    }

    pointerInput(enabled) {
        detectHorizontalDragGestures(
            onDragStart = { offset ->
                startedAtEdge = !requireEdge || (offset.x <= edgeWidthPx)
                dragPx = 0f
                committed = false
                velocityTracker.resetTracking()
            },
            onHorizontalDrag = { change, dragAmount ->
                if (startedAtEdge && !committed) {
                    dragPx = (dragPx + dragAmount).coerceAtLeast(0f)
                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                    val commitThreshold = max(thresholdPx, size.width * 0.25f)
                    if (dragPx >= commitThreshold) tryCommit()
                }
            },
            onDragEnd = {
                if (startedAtEdge && !committed) {
                    val vx = velocityTracker.calculateVelocity().x
                    if (vx >= flingVelocityPx) tryCommit()
                    else scope.launch {
                        animate(dragPx, 0f, animationSpec = tween(200)) { v, _ -> dragPx = v }
                    }
                }
                startedAtEdge = false
            },
            onDragCancel = {
                if (!committed) {
                    scope.launch {
                        animate(dragPx, 0f, animationSpec = tween(200)) { v, _ -> dragPx = v }
                    }
                }
                startedAtEdge = false
            }
        )
    }.graphicsLayer {
        translationX = dragPx.coerceIn(0f, size.width.toFloat())
    }
}
