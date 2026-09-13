package com.jetbrains.kmpapp.screens.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Послойная навигация «родитель под дочерним» вместо AnimatedContent:
 *  - родитель всегда скомпонован ПОД дочерним экраном — при свайпе он
 *    виден сразу, «мёртвой зоны» и однотонной подложки нет;
 *  - анимируется ОДИН слой (graphicsLayer translationX) — на Android
 *    это радикально дешевле, чем две анимируемые композиции
 *    AnimatedContent (меньше «рваности»);
 *  - вперёд — мягкий въезд справа (280 мс), назад — продолжение жеста
 *    (350 мс, линейный старт → плавное торможение);
 *  - быстрый короткий флик — сразу назад; медленный малый сдвиг —
 *    отскок; дотянул четверть экрана — назад.
 *
 * `screen == null` — корень; иначе сверху дочерний слой, под ним
 * `parentScreen` (или корень). Все кнопки «назад» зову́т один и тот же
 * `back` — выход к родителю через анимацию.
 */
@Composable
fun LayeredNavHost(
    screen: Any?,
    parentScreen: Any?,
    onBackToParent: () -> Unit,
    modifier: Modifier = Modifier,
    rootContent: @Composable () -> Unit,
    screenContent: @Composable (screen: Any?, back: () -> Unit) -> Unit
) {
    BoxWithConstraints(modifier.fillMaxSize().clipToBounds()) {
        val widthPx = constraints.maxWidth.toFloat()
        val scope = rememberCoroutineScope()

        if (screen == null) {
            rootContent()
            return@BoxWithConstraints
        }

        // Слой родителя — статичен, виден при сдвиге дочернего.
        if (parentScreen != null) {
            // back недостижим: родитель полностью закрыт дочерним экраном.
            screenContent(parentScreen) {}
        } else {
            rootContent()
        }

        var isBackTransition by remember { mutableStateOf(false) }
        var committed by remember(screen) { mutableStateOf(false) }
        val layerX = remember(screen) { mutableFloatStateOf(widthPx) }

        val back: (Float) -> Unit = { initialVelocity ->
            committed = true
            scope.launch {
                animate(
                    layerX.floatValue, widthPx, initialVelocity,
                    animationSpec = tween(300, easing = LinearOutSlowInEasing)
                ) { v, _ -> layerX.floatValue = v }
                isBackTransition = true
                onBackToParent()
            }
        }

        LaunchedEffect(screen) {
            if (isBackTransition) {
                // Родитель уже под пальцем был виден — новой анимации входа нет.
                layerX.floatValue = 0f
                isBackTransition = false
            } else {
                animate(
                    layerX.floatValue, 0f,
                    animationSpec = tween(280, easing = FastOutSlowInEasing)
                ) { v, _ -> layerX.floatValue = v }
            }
        }

        androidx.compose.foundation.layout.Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = layerX.floatValue.coerceIn(0f, widthPx) }
                .swipeBackLayer(
                    layerX = layerX,
                    widthPx = widthPx,
                    isCommitted = { committed },
                    onCommit = { velocity -> back(velocity) }
                )
        ) {
            screenContent(screen) { back(0f) }
        }
    }
}

/**
 * Жест сдвига дочернего слоя. Пишет прямо в layerX хоста:
 * палец двигает слой 1:1, коммит — флик по скорости или четверть экрана,
 * иначе отскок на место.
 */
private fun Modifier.swipeBackLayer(
    layerX: androidx.compose.runtime.MutableFloatState,
    widthPx: Float,
    isCommitted: () -> Boolean,
    onCommit: (velocityPxPerSec: Float) -> Unit
): Modifier = composed {
    var startedAtEdge by remember { mutableStateOf(false) }
    val velocityTracker = remember { VelocityTracker() }
    val scope = rememberCoroutineScope()
    val flingVelocityPx = with(LocalDensity.current) { 800.dp.toPx() }

    pointerInput(Unit) {
        detectHorizontalDragGestures(
            onDragStart = { offset ->
                startedAtEdge = offset.x <= 200f
                velocityTracker.resetTracking()
            },
            onHorizontalDrag = { change, dragAmount ->
                if (startedAtEdge && !isCommitted()) {
                    layerX.floatValue = (layerX.floatValue + dragAmount).coerceIn(0f, widthPx)
                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                    if (layerX.floatValue >= max(120f, widthPx * 0.25f)) {
                        onCommit(0f)
                    }
                }
            },
            onDragEnd = {
                if (startedAtEdge && !isCommitted()) {
                    val vx = velocityTracker.calculateVelocity().x
                    if (vx >= flingVelocityPx) {
                        onCommit(vx)
                    } else {
                        scope.launch {
                            animate(
                                layerX.floatValue, 0f,
                                animationSpec = tween(200)
                            ) { v, _ -> layerX.floatValue = v }
                        }
                    }
                }
                startedAtEdge = false
            },
            onDragCancel = {
                if (startedAtEdge && !isCommitted()) {
                    scope.launch {
                        animate(
                            layerX.floatValue, 0f,
                            animationSpec = tween(200)
                        ) { v, _ -> layerX.floatValue = v }
                    }
                }
                startedAtEdge = false
            }
        )
    }
}
