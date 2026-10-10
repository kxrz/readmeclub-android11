package club.readme.android.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MinesTest {

    @Test fun firstTapIsSafeAndOpensAnArea() {
        repeat(20) { seed ->
            val m = Mines.forSize(7)
            m.open(24, Random(seed))
            assertEquals(Mines.State.PLAYING == m.state || Mines.State.WON == m.state, true)
            assertFalse(m.isMine(24))
            assertTrue(m.neighbours(24).none { m.isMine(it) })
            assertTrue(m.open.count { it } > 1)
            assertEquals(8, (0 until 49).count { m.isMine(it) })
        }
    }

    @Test fun openingAMineLoses() {
        val m = Mines.forSize(7)
        m.open(0, Random(1))
        val mine = (0 until 49).first { m.isMine(it) }
        m.open(mine)
        assertEquals(Mines.State.LOST, m.state)
    }

    @Test fun openingEverySafeCellWins() {
        val m = Mines.forSize(7)
        m.open(0, Random(2))
        for (i in 0 until 49) if (!m.isMine(i)) m.open(i)
        assertEquals(Mines.State.WON, m.state)
    }

    @Test fun flaggedCellsDoNotOpen() {
        val m = Mines.forSize(7)
        m.toggleFlag(10)
        m.open(10, Random(3))
        assertFalse(m.open[10])
        assertEquals(1, m.flags)
    }
}
