package com.jetbrains.kmpapp.data.storage

import platform.Foundation.NSUserDefaults

/**
 * iOS: standard defaults содержит и системные ключи (AppleLanguages,
 * NSInterface и т.п.) — учитывать их в «памяти приложения» нельзя, это
 * не наши данные. Берём только свои: все ключи приложения начинаются с
 * «krasava_» либо «games_» (рекорды и настройки игр).
 */
actual fun PlatformStorage.allEntries(): List<StorageEntry> {
    val defaults = NSUserDefaults.standardUserDefaults
    val result = mutableListOf<StorageEntry>()
    defaults.dictionaryRepresentation().forEach { (rawKey, value) ->
        val key = rawKey as? String ?: return@forEach
        if (!key.startsWith("krasava_") && !key.startsWith("games_")) return@forEach
        (value as? String)?.let { result += StorageEntry(key, it) }
    }
    return result
}