package com.jetbrains.kmpapp.screens.other

/**
 * Команда проекта — декларативный список в коде (не «конструктор»):
 * новые участники = одна строка в списке ниже.
 */
enum class TeamRank(val label: String) {
    OWNER("Владелец"),
    CORE("Команда"),
    CONTRIBUTOR("Контрибьютор"),
    ALUMNI("Участник прошлого"),
    THANKS("Благодарности")
}

data class TeamMember(
    val name: String,
    val role: String,
    val rank: TeamRank,
    val description: String? = null,
    val githubUrl: String? = null
)

val projectTeam = listOf(
    TeamMember(
        name = "l1ratch",
        role = "Разработка и поддержка",
        rank = TeamRank.OWNER,
        githubUrl = "https://github.com/l1ratch"
    )
)
