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