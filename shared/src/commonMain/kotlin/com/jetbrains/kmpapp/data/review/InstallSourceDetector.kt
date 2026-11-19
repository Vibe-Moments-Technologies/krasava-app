package com.jetbrains.kmpapp.data.review

/**
 * Откуда установлено приложение — чтобы просить отзыв только там, где
 * пользователь действительно сможет его оставить.
 *
 * Android читает установщика через PackageManager. Точное имя пакета RuStore
 * в его документации не опубликовано, поэтому матчим известные имена И
 * подстроку «rustore» — это переживёт ребрендинг пакета. Фактический
 * установщик виден в отладочном меню («О программе», 8 тапов → строка
 * «установщик»): если RuStore окажется под другим именем, добавим его сюда.
 *
 * iOS: приложение в App Store не публиковалось, сборки идут через sideload
 * IPA — отзыв в магазине оставить нельзя, там всегда SIDELOAD.
 */
expect fun detectInstallSource(): InstallSource

/** Разбор имени пакета установщика (чистая функция — покрыта тестами). */
internal fun installSourceFromInstallerPackage(installer: String?): InstallSource {
    if (installer.isNullOrBlank()) return InstallSource.SIDELOAD
    val normalized = installer.lowercase()
    return when {
        normalized == "com.android.vending" -> InstallSource.GOOGLE_PLAY
        // Известные имена магазина + запас на будущее: любой пакет с rustore.
        normalized.contains("rustore") ||
            normalized == "ru.vcmob.store" ||
            normalized == "ru.vk.store" -> InstallSource.RU_STORE
        else -> InstallSource.SIDELOAD
    }
}