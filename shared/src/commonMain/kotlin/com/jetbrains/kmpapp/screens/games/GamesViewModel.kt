package com.jetbrains.kmpapp.screens.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.kmpapp.data.model.DateUtils
import com.jetbrains.kmpapp.data.storage.GameRecord
import com.jetbrains.kmpapp.data.storage.GamesStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Clock

/**
 * Раздел «Игры»: меню выбора игры и состояние партии в «Сапере».
 * Вся логика правил — в [MinesweeperEngine], здесь только хранение состояния,
 * таймер и рекорды.
 */
class GamesViewModel(private val gamesStorage: GamesStorage) : ViewModel() {

    /** Открытая игра (null — открыто меню «Игры»). */
    enum class Game { MINESWEEPER }

    private val _activeGame = MutableStateFlow<Game?>(null)
    val activeGame: StateFlow<Game?> = _activeGame.asStateFlow()

    private val _difficulty = MutableStateFlow(MinesweeperDifficulty.STANDARD)
    val difficulty: StateFlow<MinesweeperDifficulty> = _difficulty.asStateFlow()

    /** null, пока не сделан первый ход: мины раскладываются в момент клика. */
    private val _board = MutableStateFlow<MinesweeperBoard?>(null)
    val board: StateFlow<MinesweeperBoard?> = _board.asStateFlow()

    private val _status = MutableStateFlow(MinesweeperStatus.READY)
    val status: StateFlow<MinesweeperStatus> = _status.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    /** Режим флага: тап по клетке ставит флаг, а не открывает её. */
    private val _flagMode = MutableStateFlow(false)
    val flagMode: StateFlow<Boolean> = _flagMode.asStateFlow()

    /**
     * «Тема Error». Включается для варианта, решённого с одного первого
     * нажатия (рекорд 0 с): такой режим в меню превращается в кнопку
     * «ERROR», а поле рисуется как красный глюк.
     */
    private val _errorTheme = MutableStateFlow(false)
    val errorTheme: StateFlow<Boolean> = _errorTheme.asStateFlow()

    /** Клетка, на которой подорвались: для каскадной анимации вскрытия мин. */
    private val _boomIndex = MutableStateFlow<Int?>(null)
    val boomIndex: StateFlow<Int?> = _boomIndex.asStateFlow()

    /**
     * Рекорды всех вариантов «Сапера»: ключ — [recordKey] варианта,
     * значение — время и дата постановки. Меню показывает рекорд каждой
     * строки сразу, поэтому все ключи прогружаются один раз при старте.
     */
    val records: StateFlow<Map<String, GameRecord>> = gamesStorage.records

    private var timerJob: Job? = null

    init {
        // Чтение с устройства не блокирует главный поток.
        viewModelScope.launch(Dispatchers.Default) {
            MinesweeperDifficulty.entries.forEach { gamesStorage.record(recordKey(it)) }
        }
    }

    // Навигация

    /**
     * Открывает «Сапер» на выбранной сложности: поле и таймер сбрасываются.
     * [errorTheme] ставится, когда вариант уже «сломан» (рекорд 0 с) — поле
     * рендерится в теме Error.
     */
    fun openGame(value: MinesweeperDifficulty, errorTheme: Boolean = false) {
        _difficulty.value = value
        _errorTheme.value = errorTheme
        resetGame()
        _activeGame.value = Game.MINESWEEPER
    }

    /** Выход из партии в меню «Игры»: поле и таймер сбрасываются. */
    fun closeGame() {
        stopTimer()
        _activeGame.value = null
        _errorTheme.value = false
        resetGame()
    }

    /** Новая партия на том же размере поля. */
    fun restart() {
        resetGame()
    }

    private fun resetGame() {
        stopTimer()
        _board.value = null
        _status.value = MinesweeperStatus.READY
        _elapsedSeconds.value = 0
        _boomIndex.value = null
    }

    fun toggleFlagMode() {
        _flagMode.value = !_flagMode.value
    }

    // Ходы

    /** Тап по клетке: раскрывает её, либо ставит флаг в режиме флага. */
    fun onCellClick(index: Int) {
        val width = _difficulty.value.width
        val x = index % width
        val y = index / width
        if (_flagMode.value) {
            onCellLongClick(index)
            return
        }
        onCellClick(x, y, chord = false)
    }

    /**
     * Тап по клетке с раскрытием области вокруг числа (chord): если вокруг
     * числовой клетки помечены ровно столько флагов, сколько вокруг мин, то
     * раскрываются все непомеченные соседи.
     */
    fun onCellClick(x: Int, y: Int, chord: Boolean) {
        val current = _board.value ?: return onFirstClick(x, y)
        if (_status.value == MinesweeperStatus.WON || _status.value == MinesweeperStatus.LOST) return
        if (chord && tryChord(current, x, y)) return
        revealAt(current, x, y)
    }

    /** Долгое нажатие: поставить или снять флаг. */
    fun onCellLongClick(index: Int) {
        val x = index % _difficulty.value.width
        val y = index / _difficulty.value.width
        val current = _board.value ?: return
        if (_status.value == MinesweeperStatus.WON || _status.value == MinesweeperStatus.LOST) return
        _board.value = MinesweeperEngine.toggleFlag(current, x, y)
    }

    /** Первый ход: мины раскладываются так, чтобы клик и его соседи были чистыми. */
    private fun onFirstClick(x: Int, y: Int) {
        val difficulty = _difficulty.value
        if (x !in 0 until difficulty.width || y !in 0 until difficulty.height) return
        val fresh = MinesweeperEngine.generate(
            width = difficulty.width,
            height = difficulty.height,
            mines = difficulty.mines,
            safeX = x,
            safeY = y,
            random = Random.Default
        )
        _board.value = fresh
        revealAt(fresh, x, y)
    }

    private fun revealAt(board: MinesweeperBoard, x: Int, y: Int) {
        val updated = MinesweeperEngine.reveal(board, x, y)
        _board.value = updated
        val cell = updated[x, y]
        _status.value = when {
            cell.isMine -> {
                _boomIndex.value = updated.index(x, y)
                MinesweeperStatus.LOST
            }
            updated.isWon -> MinesweeperStatus.WON
            else -> MinesweeperStatus.PLAYING
        }
        when (_status.value) {
            MinesweeperStatus.PLAYING -> startTimer()
            MinesweeperStatus.WON -> {
                _board.value = MinesweeperEngine.flagRemainingMines(updated)
                stopTimer()
                saveResultIfBest()
            }
            MinesweeperStatus.LOST -> stopTimer()
            MinesweeperStatus.READY -> stopTimer()
        }
    }

    /** Двойной тап по числу: раскрыть остаток области, если вокруг все флаги. */
    fun onCellDoubleClick(index: Int) {
        val width = _difficulty.value.width
        onCellClick(index % width, index / width, chord = true)
    }

    /** Раскрытие вокруг числа: все флаги вокруг уже расставлены — открываем остальное. */
    private fun tryChord(board: MinesweeperBoard, x: Int, y: Int): Boolean {
        val cell = board[x, y]
        if (!cell.isRevealed || cell.adjacentMines == 0) return false
        val neighbours = ArrayList<MinesweeperCell>(8)
        forEachNeighbour(board, x, y) { neighbours.add(it) }
        if (neighbours.count { it.isFlagged } != cell.adjacentMines) return false
        neighbours.forEach { neighbour ->
            // Партия могла закончиться на предыдущем соседе — дальше не идём,
            // иначе статус вернулся бы из LOST/WON в PLAYING.
            val status = _status.value
            if (status == MinesweeperStatus.WON || status == MinesweeperStatus.LOST) return@forEach
            if (!neighbour.isRevealed && !neighbour.isFlagged) {
                revealAt(_board.value ?: board, neighbour.x, neighbour.y)
            }
        }
        return true
    }

    private inline fun forEachNeighbour(
        board: MinesweeperBoard,
        x: Int,
        y: Int,
        action: (MinesweeperCell) -> Unit
    ) {
        for (dy in -1..1) {
            for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx
                val ny = y + dy
                if (board.inBounds(nx, ny)) action(board[nx, ny])
            }
        }
    }

    private fun saveResultIfBest() {
        val difficulty = _difficulty.value
        val seconds = _elapsedSeconds.value
        // «0 с» — тоже рекорд: поле, решённое с первого же нажатия, «ломает»
        // режим и превращает его кнопку в меню в «ERROR».
        gamesStorage.submitResult(
            key = recordKey(difficulty),
            seconds = seconds,
            achievedAtMillis = Clock.System.now().toEpochMilliseconds()
        )
    }

    // Таймер

    private fun startTimer() {
        if (timerJob != null) return
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                _elapsedSeconds.value += 1
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        stopTimer()
        super.onCleared()
    }

    companion object {
        private const val GAME = "minesweeper"

        fun recordKey(difficulty: MinesweeperDifficulty): String = "$GAME${difficulty.name}"

        /** «35 с · 06.10.2026 14:23» для меню. */
        fun formatRecord(record: GameRecord): String =
            "${record.seconds} с · ${DateUtils.formatDateTime(record.achievedAtMillis)}"
    }
}