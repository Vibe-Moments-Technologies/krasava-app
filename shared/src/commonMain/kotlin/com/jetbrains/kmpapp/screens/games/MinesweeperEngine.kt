package com.jetbrains.kmpapp.screens.games

import kotlin.random.Random

/**
 * Варианты «Сапёра»: размер поля, число мин и подпись в меню игр.
 * Порядок перечисления совпадает с порядком в меню. Внутреннее имя
 * [name] используется как ключ рекорда, поэтому переименовывать
 * значения без нужды нельзя — старый рекорд станет недоступным.
 */
enum class MinesweeperDifficulty(
    val title: String,
    val width: Int,
    val height: Int,
    val mines: Int
) {
    KINDERGARTEN("Для детского сада (надеюсь, ты шутишь)", 5, 5, 3),
    SCHOOL("Для школьников (ты точно целевая аудитория?)", 7, 7, 5),
    STANDARD("Для абитуриентов (выбери сложнее)", 9, 9, 10),
    BACHELOR("Для бакалавров (хотя бы так)", 16, 16, 40),
    SPECIALIST("Для специалистов (с этим уже можно играть)", 30, 16, 99),
    MASTER("Магистр (Уважаем)", 25, 25, 100),
    POSTGRAD("Аспирант (Ты достиг высокого духовного уровня)", 50, 50, 250),
    RETIREE("Для пенсионеров (Красава... Ты с ума сошел?)", 64, 48, 777)
}

/** Состояние партии. READY — мины ещё не расставлены, первый ход впереди. */
enum class MinesweeperStatus {
    READY,
    PLAYING,
    WON,
    LOST
}

data class MinesweeperCell(
    val x: Int,
    val y: Int,
    val isMine: Boolean,
    val adjacentMines: Int,
    val isRevealed: Boolean = false,
    val isFlagged: Boolean = false
)

data class MinesweeperBoard(
    val width: Int,
    val height: Int,
    val cells: List<MinesweeperCell>
) {
    fun inBounds(x: Int, y: Int): Boolean = x in 0 until width && y in 0 until height

    fun index(x: Int, y: Int): Int = y * width + x

    operator fun get(x: Int, y: Int): MinesweeperCell = cells[index(x, y)]

    val mineCount: Int get() = cells.count { it.isMine }

    val flagCount: Int get() = cells.count { it.isFlagged }

    /** Остаток мин = всего мин − выставленных флагов (может уйти в минус). */
    val remainingMines: Int get() = mineCount - flagCount

    val revealedSafeCount: Int get() = cells.count { it.isRevealed && !it.isMine }

    val safeCellCount: Int get() = cells.size - mineCount

    /** Победа: открыты все клетки без мин. */
    val isWon: Boolean get() = safeCellCount > 0 && revealedSafeCount == safeCellCount
}

/**
 * Правила «Сапёра» как чистые функции: расстановка мин, раскрытие области
 * вокруг нуля, флаги, проверка победы. Без Compose и без состояния — чтобы
 * поведение можно было проверить обычными тестами.
 */
object MinesweeperEngine {

    /**
     * Расставляет [mines] мин случайно. Вокруг клетки первого хода
     * ([safeX], [safeY]) мины не ставятся: первый клик всегда безопасный
     * и с него открывается область — как в классическом «Сапёре».
     */
    fun generate(
        width: Int,
        height: Int,
        mines: Int,
        safeX: Int = -1,
        safeY: Int = -1,
        random: Random = Random.Default
    ): MinesweeperBoard {
        require(width > 0 && height > 0) { "Некорректный размер поля ${width}x$height" }
        val total = width * height
        val safeZone = HashSet<Int>()
        if (safeX in 0 until width && safeY in 0 until height) {
            for (dy in -1..1) {
                for (dx in -1..1) {
                    val nx = safeX + dx
                    val ny = safeY + dy
                    if (nx in 0 until width && ny in 0 until height) {
                        safeZone.add(ny * width + nx)
                    }
                }
            }
        }
        // Столько мин, сколько вообще помещается вне безопасной зоны.
        val candidates = (0 until total).filter { it !in safeZone }.toMutableList()
        val mineCount = mines.coerceIn(0, candidates.size)
        candidates.shuffle(random)
        val mineIndexes = HashSet<Int>()
        for (i in 0 until mineCount) {
            mineIndexes.add(candidates[i])
        }
        val cells = ArrayList<MinesweeperCell>(total)
        for (index in 0 until total) {
            val x = index % width
            val y = index / width
            val isMine = index in mineIndexes
            var adjacent = 0
            if (!isMine) {
                neighbours(width, height, x, y).forEach { (nx, ny) ->
                    if (ny * width + nx in mineIndexes) adjacent++
                }
            }
            cells.add(MinesweeperCell(x = x, y = y, isMine = isMine, adjacentMines = adjacent))
        }
        return MinesweeperBoard(width = width, height = height, cells = cells)
    }

    /**
     * Открывает клетку. На пустой (0) клетке раскрывается вся связанная
     * область — как в оригинале. На мине партия проигрывается: вскрываются
     * все мины. Открытая и помеченная флагом клетки не меняются.
     */
    fun reveal(board: MinesweeperBoard, x: Int, y: Int): MinesweeperBoard {
        if (!board.inBounds(x, y)) return board
        val target = board[x, y]
        if (target.isRevealed || target.isFlagged) return board
        if (target.isMine) {
            return board.copy(
                cells = board.cells.map { cell ->
                    if (cell.isMine) cell.copy(isRevealed = true) else cell
                }
            )
        }
        val cells = board.cells.toMutableList()
        val visited = HashSet<Int>()
        val stack = ArrayDeque<Int>()
        stack.addLast(board.index(x, y))
        while (stack.isNotEmpty()) {
            val index = stack.removeLast()
            if (!visited.add(index)) continue
            val cell = cells[index]
            if (cell.isRevealed || cell.isFlagged) continue
            cells[index] = cell.copy(isRevealed = true)
            // Область раскрывается только с нулевой клетки.
            if (cell.adjacentMines == 0) {
                neighbours(board.width, board.height, cell.x, cell.y).forEach { (nx, ny) ->
                    val next = ny * board.width + nx
                    if (next !in visited) stack.addLast(next)
                }
            }
        }
        return board.copy(cells = cells)
    }

    /** Ставит или снимает флаг. Открытые клетки не помечаются. */
    fun toggleFlag(board: MinesweeperBoard, x: Int, y: Int): MinesweeperBoard {
        if (!board.inBounds(x, y)) return board
        val cell = board[x, y]
        if (cell.isRevealed) return board
        val cells = board.cells.toMutableList()
        cells[board.index(x, y)] = cell.copy(isFlagged = !cell.isFlagged)
        return board.copy(cells = cells)
    }

    /** При победе оставшиеся мины помечаются флагами — так в классической игре. */
    fun flagRemainingMines(board: MinesweeperBoard): MinesweeperBoard =
        board.copy(
            cells = board.cells.map { cell ->
                if (cell.isMine && !cell.isFlagged && !cell.isRevealed) {
                    cell.copy(isFlagged = true)
                } else {
                    cell
                }
            }
        )

    /** Соседние клетки (8 штук) в границах поля. */
    private fun neighbours(width: Int, height: Int, x: Int, y: Int): List<Pair<Int, Int>> {
        val result = ArrayList<Pair<Int, Int>>(8)
        for (dy in -1..1) {
            for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height) {
                    result.add(nx to ny)
                }
            }
        }
        return result
    }
}