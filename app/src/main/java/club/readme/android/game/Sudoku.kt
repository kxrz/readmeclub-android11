package club.readme.android.game

import kotlin.random.Random

/**
 * Sudoku on a [size] × [size] grid (6 × 6 with 2 × 3 boxes, or 9 × 9 with 3 × 3 boxes).
 * Cells hold 0 for empty, else 1..size. A puzzle is a full random solution with cells
 * removed while the solution stays unique.
 */
class Sudoku(
    val size: Int,
    val solution: IntArray,
    /** True where the puzzle gives the digit (it cannot be changed). */
    val given: BooleanArray,
    /** The player's grid, givens included. */
    val cells: IntArray,
) {
    val boxRows: Int get() = boxRows(size)
    val boxCols: Int get() = size / boxRows

    val filled: Int get() = cells.count { it != 0 }
    val solved: Boolean get() = cells.contentEquals(solution)

    fun set(index: Int, digit: Int) {
        if (!given[index]) cells[index] = digit
    }

    /** Cells holding a digit that differs from the solution. */
    fun errors(): Int = cells.indices.count { cells[it] != 0 && cells[it] != solution[it] }

    enum class Level(val clues6: Int, val clues9: Int) { EASY(20, 40), MEDIUM(16, 32), HARD(13, 26) }

    companion object {
        fun boxRows(size: Int) = if (size == 6) 2 else 3

        fun generate(size: Int, level: Level, random: Random = Random.Default): Sudoku {
            val solution = IntArray(size * size)
            fill(solution, size, 0, random)
            val puzzle = solution.copyOf()
            val target = if (size == 6) level.clues6 else level.clues9
            var clues = size * size
            for (index in (0 until size * size).shuffled(random)) {
                if (clues <= target) break
                val kept = puzzle[index]
                puzzle[index] = 0
                if (countSolutions(puzzle.copyOf(), size, 2) == 1) clues-- else puzzle[index] = kept
            }
            return Sudoku(size, solution, BooleanArray(size * size) { puzzle[it] != 0 }, puzzle)
        }

        /** Fills [grid] from [index] on with a random valid completion; false if none exists. */
        private fun fill(grid: IntArray, size: Int, index: Int, random: Random): Boolean {
            if (index == grid.size) return true
            if (grid[index] != 0) return fill(grid, size, index + 1, random)
            for (digit in (1..size).shuffled(random)) {
                if (allowed(grid, size, index, digit)) {
                    grid[index] = digit
                    if (fill(grid, size, index + 1, random)) return true
                    grid[index] = 0
                }
            }
            return false
        }

        /** Number of solutions of [grid], counting no further than [limit]. */
        fun countSolutions(grid: IntArray, size: Int, limit: Int): Int {
            val index = grid.indexOf(0)
            if (index < 0) return 1
            var count = 0
            for (digit in 1..size) {
                if (allowed(grid, size, index, digit)) {
                    grid[index] = digit
                    count += countSolutions(grid, size, limit - count)
                    grid[index] = 0
                    if (count >= limit) break
                }
            }
            return count
        }

        fun allowed(grid: IntArray, size: Int, index: Int, digit: Int): Boolean {
            val row = index / size
            val col = index % size
            val br = boxRows(size)
            val bc = size / br
            for (i in 0 until size) {
                if (grid[row * size + i] == digit || grid[i * size + col] == digit) return false
            }
            val r0 = row / br * br
            val c0 = col / bc * bc
            for (r in r0 until r0 + br) for (c in c0 until c0 + bc) if (grid[r * size + c] == digit) return false
            return true
        }
    }
}
