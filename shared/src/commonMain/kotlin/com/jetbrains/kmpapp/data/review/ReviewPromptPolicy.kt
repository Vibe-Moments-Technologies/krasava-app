package com.jetbrains.kmpapp.data.review

/**
 * Когда просить пользователя поставить звезду на GitHub и отзыв в магазине.
 *
 * Запрос владельца: «если человек заходит в приложение 7 раз, то на 8-й
 * откроется всплывающее предложение». Просьба показывается ОДИН раз: после
 * любого ответа (звезда, отзыв, «позже», «не надо») больше не появляется —
 * навязчивость хуже, чем отсутствие отзыва.
 *
 * Отложить можно: тогда просьба вернётся через [SNOOZE_LAUNCHES] запусков,
 * но не больше [MAX_SNOOZES] раз — иначе «позже» превращается в вечный показ.
 */
object ReviewPromptPolicy {
    /** На каком по счёту запуске показать просьбу (владелец: на 8-м). */
    const val SHOW_ON_LAUNCH = 8

    /** Сколько запусков подождать после «позже». */
    const val SNOOZE_LAUNCHES = 10

    /** Сколько раз разрешено откладывать, прежде чем просьба уйдёт навсегда. */
    const val MAX_SNOOZES = 2

    /**
     * Показывать ли просьбу на этом запуске.
     *
     * [launchCount] — сколько раз приложение открывали, включая текущий запуск.
     * [resolved] — пользователь уже ответил (звезда/отзыв/«не надо»): тогда
     * просьба закрыта навсегда.
     * [snoozeUntilLaunch] — запуск, начиная с которого можно показать снова
     * после «позже»; null — откладываний не было.
     * [snoozeCount] — сколько раз уже откладывали.
     */
    fun shouldShow(
        launchCount: Int,
        resolved: Boolean,
        snoozeUntilLaunch: Int?,
        snoozeCount: Int
    ): Boolean {
        if (resolved) return false
        if (launchCount < SHOW_ON_LAUNCH) return false
        if (snoozeCount >= MAX_SNOOZES) return false
        // После «позже» ждём заданное число запусков.
        if (snoozeUntilLaunch != null && launchCount < snoozeUntilLaunch) return false
        return true
    }

    /** До какого запуска отложить просьбу при «позже». */
    fun snoozeUntil(launchCount: Int): Int = launchCount + SNOOZE_LAUNCHES
}

/**
 * Куда вести пользователя за отзывом — зависит от того, откуда приложение
 * установлено. Просить отзыв в магазине, из которого человек не ставил
 * приложение, бессмысленно: он там не сможет его оставить.
 */
enum class InstallSource {
    /** RuStore — есть страница приложения, отзыв можно оставить. */
    RU_STORE,

    /** Google Play — страница есть, но приложение там не публиковалось. */
    GOOGLE_PLAY,

    /** App Store (iOS) — приложение не публиковалось. */
    APP_STORE,

    /** APK/IPA вручную, GitHub, тестовая сборка — магазина нет. */
    SIDELOAD;

    /** Можно ли просить отзыв в магазине (иначе только звезда на GitHub). */
    val hasStoreListing: Boolean
        get() = this == RU_STORE
}