package com.jetbrains.kmpapp.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoffeeThemeQuestTest {

    @Test
    fun countsEachGithubRepositoryOnce() {
        var visited = emptySet<String>()
        visited = addCoffeeRepositoryVisit(visited, "https://github.com/l1ratch")!!
        visited = addCoffeeRepositoryVisit(visited, "https://github.com/L1RATCH/")!!
        assertEquals(1, visited.size)
    }

    @Test
    fun questCompletesAfterFourDistinctRepositories() {
        var visited = emptySet<String>()
        visited = addCoffeeRepositoryVisit(visited, "https://github.com/l1ratch")!!
        visited = addCoffeeRepositoryVisit(visited, "https://github.com/prosto-max")!!
        visited = addCoffeeRepositoryVisit(visited, "https://github.com/KiraSunshine")!!
        assertFalse(isCoffeeQuestComplete(visited))
        visited = addCoffeeRepositoryVisit(visited, "https://github.com/kristalina5058")!!
        assertTrue(isCoffeeQuestComplete(visited))
    }

    @Test
    fun ignoresNonGithubLinks() {
        assertNull(addCoffeeRepositoryVisit(emptySet(), "https://t.me/l1ratch"))
    }
}
