package com.jetbrains.kmpapp.data.storage

import kotlin.test.Test
import kotlin.test.assertEquals

class ScheduleTitleTest {

    @Test
    fun emptyScheduleGetsOneStar() {
        assertEquals("Математика *", uniqueStarredLocalTitle("Математика", emptySet()))
    }

    @Test
    fun duplicateGetsOneMoreStar() {
        assertEquals(
            "Математика **",
            uniqueStarredLocalTitle("Математика", setOf("Математика *"))
        )
        assertEquals(
            "Математика ***",
            uniqueStarredLocalTitle("Математика", setOf("Математика *", "Математика **"))
        )
    }
}
