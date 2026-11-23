package com.jetbrains.kmpapp.theme

enum class ThemeOverlay {
    NONE,
    SAKURA,
    KAWAII,
    CYBERPUNK,
    MATRIX,
    HALLOWEEN,
    NEW_YEAR,
    COFFEE
}

/**
 * Какая тема должна быть активна после выключения [turningOff].
 *
 * Решение владельца: выключение темы возвращает не на дефолт, а на
 * **последнюю явно выбранную** тему. Если последняя выбранная — это и есть
 * выключаемая тема (или её нет вовсе), возвращаемся на [ThemeOverlay.NONE].
 *
 * Сезонные темы сюда не попадают: они не «выбираются», а включаются
 * правилом сезона, поэтому в [lastChosen] их быть не должно.
 */
internal fun themeAfterDisable(
    turningOff: ThemeOverlay,
    lastChosen: ThemeOverlay
): ThemeOverlay =
    if (lastChosen != ThemeOverlay.NONE && lastChosen != turningOff) lastChosen
    else ThemeOverlay.NONE

/**
 * Куда возвращаться после выключения темы, если «последняя выбранная» могла
 * оказаться заблокированной.
 *
 * Зачем: ветка design_fix ужесточила кофе — она больше не «находится» сама из
 * легаси-ключа активности, а сохранённый оверлей COFFEE без разблокировки
 * откатывается на Сакуру. Но «последняя явно выбранная» тема ([lastChosen])
 * при этом не чистилась: у пользователя, который когда-то включал кофе, там
 * мог остаться COFFEE. Возврат на него вернул бы тему, которую нельзя ни
 * включить, ни выключить тумблером.
 *
 * Правило: возвращаемся на [lastChosen] только если он сейчас РАЗБЛОКИРОВАН и
 * не отозван; иначе — на [ThemeOverlay.NONE]. Проверка не меняет саму логику
 * разблокировки (она осталась как в design_fix), а только фильтрует цель
 * возврата, поэтому найденные темы не теряются.
 *
 * [unlocked] — найденные квестовые темы; [revoked] — отозванные за нарушения.
 * Темы без квеста (Сакура, сезонные) возвращать можно всегда: они не блокируются.
 */
internal fun themeReturnTarget(
    turningOff: ThemeOverlay,
    lastChosen: ThemeOverlay,
    unlocked: Set<ThemeOverlay>,
    revoked: Set<ThemeOverlay>
): ThemeOverlay {
    val candidate = themeAfterDisable(turningOff, lastChosen)
    return if (isThemeAvailable(candidate, unlocked, revoked)) candidate else ThemeOverlay.NONE
}

/** Темы, которые открываются квестом и потому могут быть заблокированы. */
private val QUEST_THEMES = setOf(
    ThemeOverlay.KAWAII,
    ThemeOverlay.CYBERPUNK,
    ThemeOverlay.MATRIX,
    ThemeOverlay.COFFEE
)

/**
 * Доступна ли тема сейчас: квестовые требуют разблокировки, отозванные
 * недоступны всегда. Сакура и сезонные не блокируются.
 *
 * Используется и при фильтрации сохранённой «предыдущей» темы на старте,
 * и при выборе цели возврата — правило одно на оба случая.
 */
internal fun isThemeAvailable(
    theme: ThemeOverlay,
    unlocked: Set<ThemeOverlay>,
    revoked: Set<ThemeOverlay>
): Boolean {
    if (theme == ThemeOverlay.NONE) return true
    if (theme in revoked) return false
    return theme !in QUEST_THEMES || theme in unlocked
}

/**
 * Какая тема рисует приложение прямо сейчас.
 *
 * Сезон СИЛЬНЕЕ явно выбранной темы (решение владельца): пока идёт сезон
 * Хэллоуина или Нового года, праздник перекрывает выбранную тему. Новый год
 * приоритетнее Хэллоуина, потому что их сезоны не пересекаются, а при
 * ручном включении одного из них «поверх» другого ожидаемо выигрывает тот,
 * что включили последним.
 */
internal fun effectiveTheme(
    chosen: ThemeOverlay,
    halloweenSeason: Boolean,
    newYearSeason: Boolean
): ThemeOverlay = when {
    newYearSeason -> ThemeOverlay.NEW_YEAR
    halloweenSeason -> ThemeOverlay.HALLOWEEN
    else -> chosen
}