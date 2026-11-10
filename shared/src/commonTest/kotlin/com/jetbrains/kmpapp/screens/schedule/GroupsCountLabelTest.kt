package com.jetbrains.kmpapp.screens.schedule

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Подпись счётчика групп в карточке пары.
 *
 * Раньше группы перечислялись названиями и обрезались («видно одну и часть
 * второй») — теперь показывается число со склонением.
 */
class GroupsCountLabelTest {

    @Test
    fun singular() {
        assertEquals("1 группа", groupsCountLabel(1))
        assertEquals("21 группа", groupsCountLabel(21))
        assertEquals("101 группа", groupsCountLabel(101))
    }

    @Test
    fun few() {
        assertEquals("2 группы", groupsCountLabel(2))
        assertEquals("3 группы", groupsCountLabel(3))
        assertEquals("4 группы", groupsCountLabel(4))
        assertEquals("22 группы", groupsCountLabel(22))
        assertEquals("34 группы", groupsCountLabel(34))
    }

    @Test
    fun many() {
        assertEquals("5 групп", groupsCountLabel(5))
        assertEquals("9 групп", groupsCountLabel(9))
        assertEquals("10 групп", groupsCountLabel(10))
        assertEquals("20 групп", groupsCountLabel(20))
    }

    /** Исключения русского счёта: 11–14 всегда «групп». */
    @Test
    fun teensAlwaysPlural() {
        assertEquals("11 групп", groupsCountLabel(11))
        assertEquals("12 групп", groupsCountLabel(12))
        assertEquals("13 групп", groupsCountLabel(13))
        assertEquals("14 групп", groupsCountLabel(14))
        assertEquals("111 групп", groupsCountLabel(111))
        assertEquals("114 групп", groupsCountLabel(114))
    }
}