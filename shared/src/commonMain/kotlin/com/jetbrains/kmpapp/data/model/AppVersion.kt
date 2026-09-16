package com.jetbrains.kmpapp.data.model

object AppVersion {
    /**
     * Базовая версия текущей линии разработки в формате YY.X.Z (старт: 26.0.0).
     * CI подставляет полный VERSION_NAME по каналу:
     *  - тег v26.0.0 / v26.0.1      → stable
     *  - тег v26.0.0-beta.3 / -rc.1  → beta / rc (prerelease)
     *  - push в main                 → 26.X-dev.N (rolling preview)
     *  - contributor build           → 26.X-contrib.N
     * v26.1.0 выпущен (сравнение расписаний, согласие, аналитика). v26.2.0 —
     * конспекты, сервисы, навигация, иконка, напоминания, фиксы недель и
     * обновлений. Текущая линия: 26.2.1 (R1 — переработка «О программе»).
     */
    const val RELEASE_VERSION = "26.2.1"
    const val VERSION_NAME = RELEASE_VERSION

    /** stable | beta | rc | dev | contrib — подставляет CI через tools/versioning.py */
    const val BUILD_CHANNEL = "stable"

    /**
     * Числовой код сборки. CI вычисляет ОДИН раз на запуск в resolve-джобе
     * (epoch-секунды) и раздаёт всем джобам через --build-id: монотонно во
     * всех каналах, влезает в Int32 / Android versionCode (max 2147483647).
     * github.run_id и github.run_started_at НЕ подходят.
     */
    const val BUILD_NUMBER = 32
    const val COMMIT_SHA = "local"

    /** Стабильный канал обновлений (обновляется только стабильными релизами). */
    const val UPDATE_FEED_URL = "https://raw.githubusercontent.com/l1ratch/MIREA-Schedule/gh-pages/version.json"

    /** Канал бета-версий (beta/rc): проверяется только если включён «Бета-канал» в настройках. */
    const val BETA_FEED_URL = "https://raw.githubusercontent.com/l1ratch/MIREA-Schedule/gh-pages/beta.json"

    const val IS_CRITICAL = false
    const val MIN_SUPPORTED_BUILD = 1
    const val CHANGELOG = "Переработан раздел «О программе»: компактная страница с командой и лицензиями на отдельных экранах. Команда — декларативный список с рангами и бейджами плюс живой список контрибьюторов GitHub. Лицензии и источники — приложение (GPL v3), библиотеки, данные и карты, юридические документы. Номер недели расписания теперь берётся из данных МИРЭА (маркеры недель из iCal), расчёт по датам остался фолбэком. Проверка обновлений работает в тестовых сборках без включённого бета-канала; кнопка обновления на Android скачивает APK напрямую вместо перехода в браузер. Исправлена нумерация недель на границе семестра (неделя с 1 сентября считалась 30-й)."

    val isTestBuild: Boolean get() = BUILD_CHANNEL != "stable"

    const val APPLICATION_ID = "ru.l1ratch.mireaschedule"
    const val DISPLAY_VERSION = "Версия $VERSION_NAME (сборка $BUILD_NUMBER)"
    const val GITHUB_REPO = "l1ratch/MIREA-Schedule"
    const val GITHUB_REPO_URL = "https://github.com/l1ratch/MIREA-Schedule"
    const val GITHUB_ISSUES_URL = "https://github.com/l1ratch/MIREA-Schedule/issues"
    const val DEVELOPER_NAME = "l1ratch"

}
