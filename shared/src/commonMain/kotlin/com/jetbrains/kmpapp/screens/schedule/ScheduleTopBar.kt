package com.jetbrains.kmpapp.screens.schedule
import com.jetbrains.kmpapp.theme.GlitchText

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.data.model.HeaderButton
import com.jetbrains.kmpapp.data.model.ScheduleDiff
import com.jetbrains.kmpapp.data.model.ScheduleTarget
import com.jetbrains.kmpapp.data.model.availableHeaderButtons
import com.jetbrains.kmpapp.data.model.badgeLabel
import com.jetbrains.kmpapp.data.model.normalizeHeaderSelection

/** Подпись кнопки шапки — общая для слота, меню и настроек. */
internal fun HeaderButton.title(): String = when (this) {
    HeaderButton.ADD -> "Добавить"
    HeaderButton.USER_LESSON -> "Своя пара"
    HeaderButton.QR -> "Сканер QR"
    HeaderButton.DAY_BADGE -> "День"
    HeaderButton.MENU -> "Меню"
}

/** Иконка кнопки; у кружка дня иконки нет — там число. */
internal fun HeaderButton.icon(): ImageVector? = when (this) {
    HeaderButton.ADD -> Icons.Default.Add
    HeaderButton.USER_LESSON -> Icons.Default.EditCalendar
    HeaderButton.QR -> Icons.Default.QrCodeScanner
    HeaderButton.DAY_BADGE -> null
    HeaderButton.MENU -> Icons.Default.MoreVert
}

/**
 * Кнопка шапки: иконка либо (для кружка дня) число в кружке.
 *
 * Одним композаблом пользуются и слот, и пузырёк — вид кнопки не разъезжается
 * между местами.
 */
@Composable
internal fun HeaderActionButton(
    button: HeaderButton,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dayNumber: Int? = null,
    onLongClick: (() -> Unit)? = null
) {
    val boxModifier = modifier.size(40.dp)
    if (button == HeaderButton.DAY_BADGE) {
        val gestures = if (onLongClick != null) {
            Modifier.pointerInput(button) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onLongClick() })
            }
        } else {
            Modifier.clickable(onClick = onClick)
        }
        Box(
            modifier = boxModifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .then(gestures),
            contentAlignment = Alignment.Center
        ) {
            GlitchText(
                text = dayNumber?.toString() ?: "•",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    } else {
        FilledTonalIconButton(onClick = onClick, modifier = boxModifier) {
            Icon(
                imageVector = button.icon() ?: Icons.Default.CalendarMonth,
                contentDescription = button.title(),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun ScheduleTopBar(
    selectedTarget: ScheduleTarget?,
    savedTargets: List<ScheduleTarget>,
    isLoading: Boolean,
    activeDiff: ScheduleDiff?,
    onSelectTarget: (ScheduleTarget) -> Unit,
    onDiffClick: () -> Unit,
    onAddClick: () -> Unit,
    onAddUserLessonClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    calendarBadgeDay: Int? = null,
    onCalendarBadgeClick: () -> Unit = {},
    onCalendarBadgeLongClick: () -> Unit = {},
    onQrScannerClick: (() -> Unit)? = null,
    headerButtons: List<HeaderButton> = HeaderButton.DEFAULT_SELECTION,
    menuOpen: Boolean = false,
    onMenuToggle: () -> Unit = {}
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    val available = availableHeaderButtons(
        hasTarget = selectedTarget != null,
        userLessonsEnabled = onAddUserLessonClick != null,
        calendarCollapsed = calendarBadgeDay != null
    )
    val visibleButtons = normalizeHeaderSelection(headerButtons, available)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Капсула группы — единственное СЖИМАЕМОЕ звено левой половины:
            // при нехватке места название уходит в многоточие, а бейдж
            // изменений сохраняет ширину и не переносится на две строки.
            Box(modifier = Modifier.weight(1f, fill = false)) {
                var capsuleWidth by remember { mutableStateOf(IntSize.Zero) }
                Row(
                    modifier = Modifier
                        .onSizeChanged { capsuleWidth = it }
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { dropdownExpanded = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlitchText(
                        text = selectedTarget?.targetTitle ?: "Выберите расписание",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Выбрать расписание",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    // Скруглённый попап: modifier-clip рисовался поверх
                    // внутренней Surface, и её углы торчали наружу.
                    // Ширина — ровно ширина капсулы, без страховочного widthIn
                    // (он побеждал width и раздувал список).
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 0.dp,
                    modifier = Modifier.then(
                        if (capsuleWidth.width > 0) {
                            Modifier.width(with(LocalDensity.current) { capsuleWidth.width.toDp() })
                        } else Modifier
                    )
                ) {
                    if (savedTargets.isEmpty()) {
                        DropdownMenuItem(
                            text = { GlitchText("Нет сохранённых групп") },
                            onClick = {
                                dropdownExpanded = false
                                onAddClick()
                            }
                        )
                    } else {
                        savedTargets.forEach { target ->
                            val isSelected = target.id == selectedTarget?.id
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            GlitchText(
                                                text = target.targetTitle,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                            GlitchText(
                                                text = target.displayTypeName,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Выбрано",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onSelectTarget(target)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (activeDiff != null && activeDiff.items.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                // «±N» вместо «Diff (N)»: короче вдвое, ширина не плавает
                // (число ограничено в badgeLabel), перенос запрещён.
                Box(
                    modifier = Modifier
                        .widthIn(min = 40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(onClick = onDiffClick)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlitchText(
                        text = activeDiff.badgeLabel(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // ПРАВАЯ группа: до трёх выбранных кнопок.
        //
        // Владелец: «выбор до 3 любых кнопок, включая меню». Меню — обычный
        // вариант: если оно выбрано, тап открывает пузырёк с НЕ выбранными
        // кнопками; если не выбрано — в шапке его нет.
        Row(verticalAlignment = Alignment.CenterVertically) {
            visibleButtons.forEachIndexed { index, button ->
                if (index > 0) Spacer(modifier = Modifier.width(8.dp))
                HeaderActionButton(
                    button = button,
                    dayNumber = calendarBadgeDay,
                    onLongClick = if (button == HeaderButton.DAY_BADGE) onCalendarBadgeLongClick else null,
                    onClick = {
                        when (button) {
                            HeaderButton.ADD -> onAddClick()
                            HeaderButton.USER_LESSON -> onAddUserLessonClick?.invoke()
                            HeaderButton.QR -> onQrScannerClick?.invoke()
                            HeaderButton.DAY_BADGE -> onCalendarBadgeClick()
                            HeaderButton.MENU -> onMenuToggle()
                        }
                    }
                )
            }
        }
    }
}

/** Носик пузырька: перевёрнутый треугольник, смотрит вниз к кнопке меню. */
private val BubbleTailShape = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width / 2f, size.height)
    close()
}

/**
 * Отступ носика от правого края пузырька — чтобы он смотрел на центр кнопки
 * меню. Вывод: padding шапки 16dp + половина кнопки 40dp = 36dp от края
 * экрана; пузырёк сдвинут на те же 16dp → 20dp внутри; минус половина
 * носика (8dp) = 12dp.
 */
private val BubbleTailEndPadding = 12.dp

/**
 * Пузырёк с кнопками — НАД областью «Ноябрь 2026 • 12 неделя» (решение
 * владельца: «строкой в виде пузырька сообщения»).
 *
 * Вызывается как оверлей поверх контента, поэтому пузырёк накрывает заголовок
 * календаря и не сдвигает ни ленту, ни список. Носик смотрит вниз к кнопке
 * меню, из которой пузырёк раскрылся. Пустой список не рисуется вовсе.
 */
@Composable
fun HeaderButtonBubble(
    buttons: List<HeaderButton>,
    onButtonClick: (HeaderButton) -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    calendarBadgeDay: Int? = null
) {
    AnimatedVisibility(
        visible = visible && buttons.isNotEmpty(),
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 6.dp,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    buttons.forEach { button ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(58.dp)
                        ) {
                            HeaderActionButton(
                                button = button,
                                dayNumber = calendarBadgeDay,
                                onClick = { onButtonClick(button) }
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            GlitchText(
                                text = button.title(),
                                fontSize = 9.sp,
                                maxLines = 2,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            // Носик смотрит точно на центр кнопки меню, а не «примерно вправо».
            //
            // Считаем от правого края экрана: у шапки padding 16dp, кнопка
            // меню 40dp и она последняя → её центр в 16 + 40/2 = 36dp от края.
            // Пузырёк выровнен по правому краю с тем же padding 16dp, значит
            // внутри пузырька центр кнопки в 36 − 16 = 20dp от его правого края.
            // Носик шириной 16dp → отступ end = 20 − 16/2 = 12dp.
            Box(
                modifier = Modifier
                    .padding(end = BubbleTailEndPadding)
                    .size(width = 16.dp, height = 7.dp)
                    .clip(BubbleTailShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            )
        }
    }
}

/**
 * Кнопки для пузырька: ВСЕ доступные, кроме выбранных в шапке.
 * МENU в пузырьке не показываем — открывать его нечем.
 */
@Composable
internal fun rememberBubbleHeaderButtons(
    hasTarget: Boolean,
    userLessonsEnabled: Boolean,
    calendarCollapsed: Boolean,
    selectedButtons: List<HeaderButton>
): List<HeaderButton> = remember(
    hasTarget, userLessonsEnabled, calendarCollapsed, selectedButtons
) {
    val available = availableHeaderButtons(hasTarget, userLessonsEnabled, calendarCollapsed)
    available.filterNot { it in selectedButtons || it == HeaderButton.MENU }
}