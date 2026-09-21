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
     * обновлений. Текущая линия: 26.3.0 — ребрендинг «Красава!», заметки к
     * парам, группировка настроек, информационные обновления. Стабильного
     * релиза 26.3.0 ещё не было (случайная публикация отозвана, version.json
     * возвращён на 26.2.0); RuStore-альфа — ранний срез линии (e6a0943).
     */
    const val RELEASE_VERSION = "26.3.0"
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
    const val UPDATE_FEED_URL = "https://raw.githubusercontent.com/Vibe-Moments-Technologies/krasava-app/gh-pages/version.json"

    const val IS_CRITICAL = false
    const val MIN_SUPPORTED_BUILD = 1
    const val CHANGELOG = "Ребрендинг: приложение теперь называется «Красава!» и переехало в организацию Vibe Moments Technologies; новый безопасный ключ подписи. Заметки к парам и предметам: пишите прямо на странице занятия — заметка видна в карточке расписания и удаляется вместе с ним. Панель инструментов на странице пары: заметки и добавление предмета в задачи. Настройки сгруппированы по разделам: отображение расписания, прогресс, календарь, параметры сервисов. ДОП-пары: розовый бейдж и тумблер «Скрывать доп. занятия». Обновления через магазин: проверка версии стала информационной. Аналитика: диагностика сбоев обязательна, события использования — по согласию; новый экран приветствия. «О программе»: команда с контактами, блок «Контакты». Новые иконки; ссылки на соцсети обновляются без обновления приложения."

    const val DISPLAY_VERSION = "Версия $VERSION_NAME (сборка $BUILD_NUMBER)"
    const val GITHUB_REPO = "Vibe-Moments-Technologies/krasava-app"
    const val GITHUB_REPO_URL = "https://github.com/Vibe-Moments-Technologies/krasava-app"
    const val GITHUB_ISSUES_URL = "https://github.com/Vibe-Moments-Technologies/krasava-app/issues"
    const val DEVELOPER_NAME = "l1ratch"

}
