package com.jetbrains.kmpapp.screens.other
import com.jetbrains.kmpapp.theme.GlitchText

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.data.model.HeaderButton

/**
 * Страница «Настройки расписания».
 *
 * Переработка (решение владельца): раньше здесь были четыре ссылки на
 * подстраницы (Отображение, Прогресс, Календарь, Кнопки шапки) — три уровня
 * вложенности и четыре тапа до любого тумблера. Теперь все настройки лежат
 * на одной странице, сгруппированные заголовками.
 *
 * Единственное исключение — «Цвета занятий»: палитра типов слишком крупная
 * для инлайна, поэтому она осталась подстраницей (согласовано с владельцем).
 */
@Composable
fun ScheduleSettingsPage(
    viewModel: OtherViewModel,
    onBack: () -> Unit,
    onOpenLessonColors: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showEmptyLessons by viewModel.showEmptyLessons.collectAsState()
    val hideAdditionalLessons by viewModel.hideAdditionalLessons.collectAsState()
    val showAbbreviatedNames by viewModel.showAbbreviatedNames.collectAsState()
    val autoScrollToCurrentLesson by viewModel.autoScrollToCurrentLesson.collectAsState()
    val showLessonProgress by viewModel.showLessonProgress.collectAsState()
    val showEmptyLessonProgress by viewModel.showEmptyLessonProgress.collectAsState()
    val showBreakProgress by viewModel.showBreakProgress.collectAsState()
    val calendarSwipeCollapse by viewModel.calendarSwipeCollapse.collectAsState()
    val userLessonsEnabled by viewModel.userLessonsEnabled.collectAsState()
    val slotButton by viewModel.headerSlotButton.collectAsState()
    val qrButtonEnabled by viewModel.qrButtonEnabled.collectAsState()
    val savedTargets by viewModel.savedTargets.collectAsState()
    val hasTarget = savedTargets.isNotEmpty()

    SettingsPageScaffold(
        title = "Настройки расписания",
        onBack = onBack,
        modifier = modifier,
        scrollKey = "schedule_settings"
    ) {
        SettingsGroup(title = "Отображение") {
            SettingsSwitchRow(
                title = "Показывать пустые пары",
                subtitle = "Отображать окна между занятиями в списке пар",
                checked = showEmptyLessons,
                onCheckedChange = viewModel::setShowEmptyLessons
            )
            SettingsRowDivider()
            SettingsSwitchRow(
                title = "Скрывать доп. занятия",
                subtitle = "Не показывать пары типа «ДОП» в расписании, календаре и напоминаниях",
                checked = hideAdditionalLessons,
                onCheckedChange = viewModel::setHideAdditionalLessons
            )
            SettingsRowDivider()
            SettingsSwitchRow(
                title = "Сокращённые названия",
                subtitle = "Сокращать по первым буквам названия предметов",
                checked = showAbbreviatedNames,
                onCheckedChange = viewModel::setShowAbbreviatedNames
            )
            SettingsRowDivider()
            SettingsSwitchRow(
                title = "Прокрутка к текущей паре",
                subtitle = "Фокусировать список на текущей паре при открытии",
                checked = autoScrollToCurrentLesson,
                onCheckedChange = viewModel::setAutoScrollToCurrentLesson
            )
        }

        SettingsGroup(title = "Прогресс") {
            SettingsSwitchRow(
                title = "Индикатор прогресса пары",
                subtitle = "Полоска оставшегося времени в карточке идущего занятия",
                checked = showLessonProgress,
                onCheckedChange = viewModel::setShowLessonProgress
            )
            SettingsRowDivider()
            SettingsSwitchRow(
                title = "Прогресс пустых пар",
                subtitle = "Полоска оставшегося времени в карточке «Нет пары»",
                checked = showEmptyLessonProgress,
                onCheckedChange = viewModel::setShowEmptyLessonProgress
            )
            SettingsRowDivider()
            SettingsSwitchRow(
                title = "Прогресс перемены",
                subtitle = "Пройденная часть надписи «перемена» подсвечивается цветом",
                checked = showBreakProgress,
                onCheckedChange = viewModel::setShowBreakProgress
            )
        }

        SettingsGroup(title = "Календарь") {
            SettingsSwitchRow(
                title = "Сворачивание свайпом",
                subtitle = "Свайп вверх по ленте календаря скрывает её над расписанием",
                checked = calendarSwipeCollapse,
                onCheckedChange = viewModel::setCalendarSwipeCollapse
            )
        }

        SettingsGroup(title = "Свои пары") {
            SettingsSwitchRow(
                title = "Редактирование расписания",
                subtitle = "Свои пары: кнопка в шапке и добавленные пары в списке",
                checked = userLessonsEnabled,
                onCheckedChange = viewModel::setUserLessonsEnabled
            )
        }

        SettingsGroup(title = "Кнопки шапки") {
            if (slotButton == null) {
                GlitchText(
                    text = "Все кнопки убраны в меню: в шапке только «⋮».",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            HeaderButton.entries.forEachIndexed { index, button ->
                if (index > 0) SettingsRowDivider()
                val available = button.availableInSettings(
                    hasTarget = hasTarget,
                    userLessonsEnabled = userLessonsEnabled,
                    qrButtonEnabled = qrButtonEnabled
                )
                HeaderButtonToggleRow(
                    button = button,
                    checked = slotButton == button && available,
                    enabled = available,
                    // Слот один: включение другой кнопки выключает предыдущую;
                    // выключение последней оставляет слот пустым.
                    onCheckedChange = { checked ->
                        viewModel.setHeaderSlotButton(if (checked) button else null)
                    }
                )
            }
        }

        // Согласованное исключение: палитра типов — подстраницей.
        SettingsGroup(title = "Оформление пар") {
            SettingsLinkRow(
                title = "Цвета занятий",
                subtitle = "Акценты для лекций, практик, лабораторных",
                icon = Icons.Default.Palette,
                onClick = onOpenLessonColors
            )
        }
    }
}