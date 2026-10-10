package club.readme.android.learn

import org.junit.Assert.assertEquals
import org.junit.Test

class LeitnerTest {

    @Test
    fun knowingACardThreeTimesRetiresIt() {
        val deck = Leitner()
        deck.add("a")
        deck.knew("a")
        assertEquals(2, deck.box("a"))
        deck.knew("a")
        assertEquals(3, deck.box("a"))
        deck.knew("a")
        assertEquals(0, deck.size)
    }

    @Test
    fun notYetGoesBackToBoxOne() {
        val deck = Leitner()
        deck.add("a")
        deck.knew("a")
        deck.notYet("a")
        assertEquals(1, deck.box("a"))
    }

    @Test
    fun higherBoxesComeUpLessOften() {
        val deck = Leitner(linkedMapOf("one" to 1, "two" to 2, "three" to 3), session = 1)
        assertEquals(listOf("one"), deck.due())
        deck.session = 2
        assertEquals(listOf("one", "two"), deck.due())
        deck.session = 4
        assertEquals(listOf("one", "two", "three"), deck.due())
    }

    @Test
    fun withNothingDueTheLowestBoxesComeUp() {
        val deck = Leitner(linkedMapOf("three" to 3, "two" to 2), session = 1)
        assertEquals(listOf("two", "three"), deck.due())
    }

    @Test
    fun survivesStorage() {
        val deck = Leitner(linkedMapOf("gk-his-001" to 2, "gk-geo-004" to 1), session = 5)
        val back = Leitner.decode(deck.encode())
        assertEquals(deck.encode(), back.encode())
        assertEquals(0, Leitner.decode("garbage").size)
        assertEquals(0, Leitner.decode(null).size)
    }
}
