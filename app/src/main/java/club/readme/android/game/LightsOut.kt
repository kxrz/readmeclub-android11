package club.readme.android.game

import kotlin.random.Random

/**
 * Lights Out: pressing a square flips it and its four neighbours; the goal is to turn every
 * square off. A puzzle is made by pressing random squares of a solved grid, so it is always
 * solvable.
 */
class LightsOut(val size: Int = 5) {

    val lit = BooleanArray(size * size)

    var moves = 0
        private set

    val solved: Boolean
        get() = lit.none { it }

    fun isLit(row: Int, col: Int) = lit[row * size + col]

    fun press(row: Int, col: Int) {
        flip(row, col)
        moves++
    }

    fun shuffle(random: Random = Random.Default) {
        do {
            for (row in 0 until size) for (col in 0 until size) if (random.nextBoolean()) flip(row, col)
        } while (solved)
        moves = 0
    }

    private fun flip(row: Int, col: Int) {
        for ((r, c) in listOf(row to col, row - 1 to col, row + 1 to col, row to col - 1, row to col + 1)) {
            if (r in 0 until size && c in 0 until size) lit[r * size + c] = !lit[r * size + c]
        }
    }
}
