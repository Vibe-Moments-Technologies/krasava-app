package com.jetbrains.kmpapp.screens.other

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TeamMembersTest {

    @Test
    fun newYearHatsMatchRoles() {
        val santaMembers = projectTeam.filter {
            it.role.contains("Разработчик") || it.role.contains("Дизайнер")
        }
        assertTrue(santaMembers.isNotEmpty())
        assertTrue(santaMembers.all { it.newYearHat == TeamNewYearHat.SANTA })

        val snowMaidenMembers = projectTeam.filter {
            it.role.contains("Картограф") && !it.role.contains("Дизайнер")
        }
        assertEquals(listOf("kristalina5058"), snowMaidenMembers.map { it.name })
        assertTrue(snowMaidenMembers.all { it.newYearHat == TeamNewYearHat.SNEGUROCHKA })
    }
}
