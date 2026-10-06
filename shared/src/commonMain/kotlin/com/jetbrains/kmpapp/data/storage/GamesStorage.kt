package com.jetbrains.kmpapp.data.storage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Рекорд по игре: время в секундах и когда оно было установлено. */
data class GameRecord(val seconds: Int, val achievedAtMillis: Long)

/**
 * Рекорды игр. Значение хранится по одному ключу на игру и уровень
 * сложности: `games_record_minesweeper_STANDARD` = `35|1759660800000`.
 *
 * Ключ известен вызывающему (имя игры + сложность), поэтому запись
 * читается лениво по ключу: [PlatformStorage] не умеет перечислять ключи.
 */
class GamesStorage(private val platformStorage: PlatformStorage) {

    // Тот же принцип, что в ScheduleStorage: своя scope на весь экран,
    // запись рекорда не должна отменять другие.
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _records = MutableStateFlow<Map<String, GameRecord>>(emptyMap())
    val records: StateFlow<Map<String, GameRecord>> = _records.asStateFlow()

    /** Рекорд по ключу; при первом обращении читается с устройства. */
    fun record(key: String): GameRecord? {
        _records.value[key]?.let { return it }
        val loaded = readRecord(key) ?: return null
        _records.value = _records.value + (key to loaded)
        return loaded
    }

    /** Лучшее время по ключу в секундах (null — рекорда ещё нет). */
    fun bestTime(key: String): Int? = record(key)?.seconds

    /**
     * Сохраняет результат, если он лучше прежнего.
     * @return true, если это новый рекорд.
     */
    fun submitResult(key: String, seconds: Int, achievedAtMillis: Long): Boolean {
        val current = record(key)
        if (current != null && current.seconds <= seconds) return false
        val updated = GameRecord(seconds, achievedAtMillis)
        _records.value = _records.value + (key to updated)
        scope.launch {
            try {
                platformStorage.saveString(storageKey(key), encode(updated))
            } catch (e: Exception) {
                println("Failed to persist game record: ${e.message}")
            }
        }
        return true
    }

    /** Удаляет все известные рекорды (используется в «Данные и кэш»). */
    fun clearRecords() {
        val keys = _records.value.keys.toList()
        _records.value = emptyMap()
        scope.launch {
            try {
                keys.forEach { platformStorage.remove(storageKey(it)) }
            } catch (e: Exception) {
                println("Failed to clear game records: ${e.message}")
            }
        }
    }

    /**
     * Удаляет только «сломанные» рекорды (0 с). Это источник темы Error:
     * как только записей с 0 с не остаётся, режим снова здоров, а кнопка
     * «ERROR» в меню превращается обратно в обычную карточку.
     */
    fun clearBrokenRecords() {
        val brokenKeys = _records.value.filterValues { it.seconds == 0 }.keys
        if (brokenKeys.isEmpty()) return
        _records.value = _records.value - brokenKeys
        scope.launch {
            try {
                brokenKeys.forEach { platformStorage.remove(storageKey(it)) }
            } catch (e: Exception) {
                println("Failed to clear broken game records: ${e.message}")
            }
        }
    }

    private fun storageKey(key: String) = "games_record_$key"

    private fun encode(record: GameRecord) = "${record.seconds}|${record.achievedAtMillis}"

    private fun readRecord(key: String): GameRecord? {
        return try {
            val raw = platformStorage.getString(storageKey(key)) ?: return null
            val parts = raw.split("|")
            if (parts.size != 2) return null
            val seconds = parts[0].trim().toIntOrNull() ?: return null
            val at = parts[1].trim().toLongOrNull() ?: return null
            if (seconds < 0) return null
            GameRecord(seconds, at)
        } catch (e: Exception) {
            println("Failed to read game record: ${e.message}")
            null
        }
    }
}