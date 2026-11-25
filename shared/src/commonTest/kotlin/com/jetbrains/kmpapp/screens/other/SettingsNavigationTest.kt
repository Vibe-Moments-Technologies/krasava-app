package com.jetbrains.kmpapp.screens.other

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Навигация переработанных настроек.
 *
 * Решение владельца: корень — плоский список разделов, внутри каждого —
 * группы настроек БЕЗ вложенных подстраниц. Единственное исключение —
 * «Цвета занятий» внутри расписания. Эксперименты и полный сброс остаются
 * в скрытом меню отладки, настройки игр — в шестерёнке внутри «Игр».
 *
 * Тесты фиксируют структуру, чтобы она случайно не «заросла» снова.
 */
class SettingsNavigationTest {

    private val sectionPages = listOf(
        OtherSubScreen.SCHEDULE_SETTINGS,
        OtherSubScreen.NOTIFICATION_SETTINGS,
        OtherSubScreen.INTERFACE_SETTINGS,
        OtherSubScreen.SERVICES_SETTINGS,
        OtherSubScreen.ADVANCED_SETTINGS,
        OtherSubScreen.DATA_AND_CACHE
    )

    @Test
    fun everySectionPageIsDirectChildOfSettings() {
        sectionPages.forEach { page ->
            assertEquals(
                OtherSubScreen.SETTINGS,
                page.parent(),
                "$page должна открываться прямо из корня настроек"
            )
        }
    }

    @Test
    fun lessonColorsIsTheOnlyNestedSettingsSubpage() {
        // Согласованное исключение: палитра типов внутри расписания.
        assertEquals(OtherSubScreen.SCHEDULE_SETTINGS, OtherSubScreen.LESSON_COLORS.parent())
    }

    @Test
    fun oldThreeLevelPagesAreGone() {
        // Раньше: Настройки → Расписание → Отображение (4 тапа до тумблера).
        val names = OtherSubScreen.entries.map { it.name }
        listOf(
            "SCHEDULE_DISPLAY", "SCHEDULE_PROGRESS", "SCHEDULE_CALENDAR",
            "HEADER_BUTTONS", "SERVICE_NOTES_SETTINGS", "DOCK_SETTINGS"
        ).forEach { obsolete ->
            assertTrue(obsolete !in names, "$obsolete должна быть удалена")
        }
    }

    @Test
    fun experimentsStayInsideHiddenDebugMenu() {
        // Решение владельца: не вытаскивать из отладки.
        assertEquals(OtherSubScreen.DEBUG_SETTINGS, OtherSubScreen.EXPERIMENTAL_SETTINGS.parent())
        assertEquals(OtherSubScreen.ABOUT, OtherSubScreen.DEBUG_SETTINGS.parent())
    }

    @Test
    fun tasksStubOpensFromServices() {
        assertEquals(OtherSubScreen.SERVICES_SETTINGS, OtherSubScreen.TASK_SETTINGS.parent())
    }

    @Test
    fun appearanceSubpagesStayAtSettingsLevel() {
        // «Внешний вид» остаётся инлайн, его подстраницы — дети корня настроек.
        assertEquals(OtherSubScreen.SETTINGS, OtherSubScreen.THEMED_THEMES.parent())
        assertEquals(OtherSubScreen.SETTINGS, OtherSubScreen.ICON_PICKER.parent())
    }

    @Test
    fun everyNonRootScreenHasParentAndChainEndsAtRoot() {
        // Битая карта родителей = экран без «назад» или пустая подложка.
        OtherSubScreen.entries.filter { it != OtherSubScreen.ROOT }.forEach { screen ->
            var current: OtherSubScreen? = screen
            val seen = mutableSetOf<OtherSubScreen>()
            while (current != null && current != OtherSubScreen.ROOT) {
                assertTrue(seen.add(current), "цикл в родителях у $screen")
                current = current.parent()
            }
            assertEquals(OtherSubScreen.ROOT, current, "цепочка $screen не доходит до корня")
        }
    }

    @Test
    fun rootHasNoParent() {
        assertNull(OtherSubScreen.ROOT.parent())
    }

    @Test
    fun notificationPageIncludesWidgetsOnlyOnAndroid() {
        // Владелец: Android — «Уведомления и виджеты», iOS — «Уведомления».
        assertEquals("Уведомления и виджеты", notificationSettingsTitle(widgetsSupported = true))
        assertEquals("Уведомления", notificationSettingsTitle(widgetsSupported = false))
    }
}
