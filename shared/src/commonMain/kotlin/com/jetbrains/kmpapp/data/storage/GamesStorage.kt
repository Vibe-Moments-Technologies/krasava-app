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

/** Снимок незавершённой партии «Сапера» для сохранения между запусками. */
data class SavedMinesweeperGame(
    val difficultyName: String,
    val width: Int,
    val height: Int,
    val elapsedSeconds: Int,
    val statusName: String,
    val boomIndex: Int?,
    val cells: List<SavedMinesweeperCell>
)

/** Клетка в сохранённой партии: те же поля, что у [com.jetbrains.kmpapp.screens.games.MinesweeperCell]. */
data class SavedMinesweeperCell(
    val x: Int,
    val y: Int,
    val isMine: Boolean,
    val adjacentMines: Int,
    val isRevealed: Boolean,
    val isFlagged: Boolean
)

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

    // Настройки «Сапера»: читаются при старте (синхронно, ключи маленькие),
    // тумблеры на экране настроек переключают их напрямую.
    private val _boardVertical = MutableStateFlow(false)
    val boardVertical: StateFlow<Boolean> = _boardVertical.asStateFlow()

    private val _smallCellHintEnabled = MutableStateFlow(false)
    val smallCellHintEnabled: StateFlow<Boolean> = _smallCellHintEnabled.asStateFlow()

    init {
        _boardVertical.value = readBool(BOARD_VERTICAL_KEY) ?: false
        _smallCellHintEnabled.value = readBool(SMALL_CELL_HINT_KEY) ?: false
    }

    /** Вертикальная ориентация поля: ширина и высота меняются местами. */
    fun setBoardVertical(enabled: Boolean) {
        if (_boardVertical.value == enabled) return
        _boardVertical.value = enabled
        persistBool(BOARD_VERTICAL_KEY, enabled)
    }

    /**
     * Подсказка «Клетки слишком мелкие — приблизите поле» при тапе по мелкой
     * клетке. По умолчанию выключена: клетки нажимаются при любом размере.
     */
    fun setSmallCellHintEnabled(enabled: Boolean) {
        if (_smallCellHintEnabled.value == enabled) return
        _smallCellHintEnabled.value = enabled
        persistBool(SMALL_CELL_HINT_KEY, enabled)
    }

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

    /**
     * Сохраняет незавершённую партию «Сапера» (не победа и не поражение).
     * Восстанавливается при открытии того же варианта, пока её не перезапустили
     * или не доиграли.
     */
    fun saveGame(game: SavedMinesweeperGame) {
        scope.launch {
            try {
                platformStorage.saveString(SAVED_GAME_KEY, encodeGame(game))
            } catch (e: Exception) {
                println("Failed to persist minesweeper game: ${e.message}")
            }
        }
    }

    /** Снимок сохранённой партии (null — сохранения нет или оно битое). */
    fun loadSavedGame(): SavedMinesweeperGame? {
        return try {
            val raw = platformStorage.getString(SAVED_GAME_KEY) ?: return null
            decodeGame(raw)
        } catch (e: Exception) {
            println("Failed to read saved minesweeper game: ${e.message}")
            null
        }
    }

    /** Стирает сохранённую партию (перезапуск, победа или поражение). */
    fun clearSavedGame() {
        scope.launch {
            try {
                platformStorage.remove(SAVED_GAME_KEY)
            } catch (e: Exception) {
                println("Failed to clear saved minesweeper game: ${e.message}")
            }
        }
    }

    private fun storageKey(key: String) = "games_record_$key"

    private fun persistBool(key: String, value: Boolean) {
        scope.launch {
            try {
                platformStorage.saveString(key, value.toString())
            } catch (e: Exception) {
                println("Failed to persist game setting: ${e.message}")
            }
        }
    }

    private fun readBool(key: String): Boolean? =
        platformStorage.getString(key)?.toBooleanStrictOrNull()

    //
    // Сохранённая партия: `mine-saved-1|вариант|ширина|высота|статус|секунды|бой?|клетки`.
    // Клетки — по 4 символа в порядке индексов: флаг, раскрыта, соседи (0-8), мина.
    //

    private fun encodeGame(game: SavedMinesweeperGame): String = buildString {
        append(SAVED_GAME_FORMAT); append('|')
        append(game.difficultyName); append('|')
        append(game.width); append('|')
        append(game.height); append('|')
        append(game.statusName); append('|')
        append(game.elapsedSeconds); append('|')
        append(game.boomIndex ?: -1); append('|')
        game.cells.forEach { cell ->
            append(if (cell.isFlagged) '1' else '0')
            append(if (cell.isRevealed) '1' else '0')
            append(cell.adjacentMines.coerceIn(0, 8))
            append(if (cell.isMine) '1' else '0')
        }
    }

    private fun decodeGame(raw: String): SavedMinesweeperGame? {
        val parts = raw.split('|')
        if (parts.size < 8 || parts[0] != SAVED_GAME_FORMAT) return null
        val width = parts[2].toIntOrNull() ?: return null
        val height = parts[3].toIntOrNull() ?: return null
        val total = width * height
        if (width <= 0 || height <= 0 || total <= 0 || total > MAX_BOARD_CELLS) return null
        val elapsed = parts[5].toIntOrNull() ?: return null
        if (elapsed < 0) return null
        val tokens = parts[7]
        if (tokens.length != total * 4) return null
        val boom = parts[6].toIntOrNull()?.takeIf { it >= 0 }
        val cells = ArrayList<SavedMinesweeperCell>(total)
        for (i in 0 until total) {
            val token = tokens.substring(i * 4, i * 4 + 4)
            val flagged = token[0] == '1'
            val revealed = token[1] == '1'
            val adjacent = token[2].digitToIntOrNull() ?: 0
            val mine = token[3] == '1'
            cells.add(
                SavedMinesweeperCell(
                    x = i % width,
                    y = i / width,
                    isMine = mine,
                    adjacentMines = adjacent,
                    isRevealed = revealed,
                    isFlagged = flagged
                )
            )
        }
        return SavedMinesweeperGame(
            difficultyName = parts[1],
            width = width,
            height = height,
            elapsedSeconds = elapsed,
            statusName = parts[4],
            boomIndex = boom,
            cells = cells
        )
    }

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

    companion object {
        private const val BOARD_VERTICAL_KEY = "games_settings_board_vertical"
        private const val SMALL_CELL_HINT_KEY = "games_settings_small_cell_hint"

        private const val SAVED_GAME_KEY = "games_minesweeper_saved_game"
        private const val SAVED_GAME_FORMAT = "mine-saved-1"

        /** Верхняя граница размера поля: 64×48 «Для пенсионеров». */
        private const val MAX_BOARD_CELLS = 64 * 48
    }
}