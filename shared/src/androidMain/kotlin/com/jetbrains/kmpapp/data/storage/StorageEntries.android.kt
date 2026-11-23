package com.jetbrains.kmpapp.data.storage

import java.io.File

/**
 * Android: перечисляем SharedPreferences, а при их недоступности —
 * файлы fallback-каталога (тот же путь, что использует [PlatformStorage]).
 *
 * Значения в этом хранилище всегда строковые (saveString), но SharedPreferences
 * формально допускает и другие типы — нестроковые пропускаем, чтобы не
 * уронить подсчёт на чужой записи.
 */
actual fun PlatformStorage.allEntries(): List<StorageEntry> {
    val context = AndroidContextProvider.context
    val prefs = context?.getSharedPreferences("krasava_schedule_cache", android.content.Context.MODE_PRIVATE)
    if (prefs != null) {
        return prefs.all.entries.mapNotNull { (key, value) ->
            (value as? String)?.let { StorageEntry(key, it) }
        }
    }
    val baseDir = context?.filesDir ?: File(System.getProperty("java.io.tmpdir") ?: ".")
    val dir = File(baseDir, "krasava_cache")
    return dir.listFiles()?.mapNotNull { file ->
        runCatching { StorageEntry(file.name, file.readText()) }.getOrNull()
    }.orEmpty()
}