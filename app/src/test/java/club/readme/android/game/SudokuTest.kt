package club.readme.android.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SudokuTest {

    private fun valid(s: Sudoku): Boolean {
        val grid = s.solution
        for (i in grid.indices) {
            val d = grid[i]
            grid[i] = 0
            val ok = Sudoku.allowed(grid, s.size, i, d)
            grid[i] = d
            if (!ok) return false
        }
        return true
    }

    @Test fun sixBySixSolutionIsValidAndUnique() {
        val s = Sudoku.generate(6, Sudoku.Level.HARD, Random(1))
        assertTrue(valid(s))
        assertEquals(1, Sudoku.countSolutions(s.cells.copyOf(), 6, 2))
        assertTrue(s.filled >= Sudoku.Level.HARD.clues6)
    }

    @Test fun nineByNineIsUniqueAtEveryLevel() {
        for (level in Sudoku.Level.values()) {
            val s = Sudoku.generate(9, level, Random(level.ordinal + 7))
            assertTrue(valid(s))
            assertEquals(1, Sudoku.countSolutions(s.cells.copyOf(), 9, 2))
        }
    }

    @Test fun givensCannotChangeAndSolvingWorks() {
        val s = Sudoku.generate(6, Sudoku.Level.EASY, Random(3))
        val g = s.given.indexOfFirst { it }
        val before = s.cells[g]
        s.set(g, if (before == 1) 2 else 1)
        assertEquals(before, s.cells[g])
        assertFalse(s.solved)
        for (i in s.cells.indices) s.set(i, s.solution[i])
        assertTrue(s.solved)
        assertEquals(0, s.errors())
    }
}
