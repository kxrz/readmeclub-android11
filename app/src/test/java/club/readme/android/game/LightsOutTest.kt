package club.readme.android.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LightsOutTest {

    @Test fun pressFlipsTheSquareAndItsNeighbours() {
        val game = LightsOut()
        game.press(2, 2)
        val expected = setOf(2 to 2, 1 to 2, 3 to 2, 2 to 1, 2 to 3)
        for (r in 0 until 5) for (c in 0 until 5) assertEquals(r to c in expected, game.isLit(r, c))
        assertEquals(1, game.moves)
    }

    @Test fun cornerPressStaysOnTheGrid() {
        val game = LightsOut()
        game.press(0, 0)
        assertEquals(3, game.lit.count { it })
    }

    @Test fun pressingTwiceUndoes() {
        val game = LightsOut()
        game.press(1, 3)
        game.press(1, 3)
        assertTrue(game.solved)
    }

    @Test fun shuffleIsUnsolvedAndResetsMoves() {
        val game = LightsOut()
        game.press(0, 0)
        game.shuffle(Random(42))
        assertFalse(game.solved)
        assertEquals(0, game.moves)
    }

    @Test fun shuffledPuzzleIsSolvable() {
        // Replaying the shuffle's presses solves it: record them with the same seed.
        val game = LightsOut()
        game.shuffle(Random(7))
        val presses = mutableListOf<Pair<Int, Int>>()
        val replay = Random(7)
        for (r in 0 until 5) for (c in 0 until 5) if (replay.nextBoolean()) presses += r to c
        presses.forEach { (r, c) -> game.press(r, c) }
        assertTrue(game.solved)
    }
}
