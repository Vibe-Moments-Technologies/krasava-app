package com.jetbrains.kmpapp.data.analytics

/**
 * Диагностика сбоев (Sentry) — opt-in: включается тумблером в скрытом меню
 * отладки, по умолчанию ВЫКЛЮЧЕНА. Пока SDK не запущен, наружу не уходит
 * ничего: Sentry не пишет и не шлёт события до init.
 *
 * Движок подставляет платформа на старте (паттерн как у прежней аналитики):
 *  - Android: ScheduleApp.onCreate → AndroidDiagnostics (sentry-android)
 *  - iOS: iOSApp.init → SentryDiagnosticsEngine (sentry-cocoa)
 */
interface DiagnosticsEngine {
    fun start(dsn: String)
    fun stop()
}

object AppDiagnostics {
    /** DSN проекта sentry.io. Пусто = диагностика не запускается даже с тумблера. */
    const val DSN = ""

    private var engine: DiagnosticsEngine? = null
    private var enabled = false

    fun setEngine(engine: DiagnosticsEngine) {
        this.engine = engine
        if (enabled) startEngine()
    }

    /** Тумблер диагностики / восстановление флага при старте. */
    fun setEnabled(value: Boolean) {
        enabled = value
        if (value) startEngine() else engine?.stop()
    }

    private fun startEngine() {
        if (DSN.isNotBlank()) engine?.start(DSN)
    }
}
