package club.readme.android.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuizRoundTest {

    private val pack = QuizPack("t", "Test", 1, (1..15).map { Question("q$it", "Theme", "Question $it?", listOf("right$it", "a", "b", "c"), "Why.") })

    @Test
    fun drawsUnseenQuestionsFirst() {
        val seen = (1..10).map { "q$it" }.toSet()
        val drawn = QuizRound.draw(pack, seen, random = Random(1))
        assertEquals(10, drawn.size)
        assertEquals((11..15).map { "q$it" }.toSet(), drawn.take(5).map { it.id }.toSet())
    }

    @Test
    fun shuffledAnswersKeepTheRightOne() {
        val round = QuizRound(pack.questions.take(10), Random(7))
        for (item in round.items) assertEquals(item.question.answers[0], item.answers[item.right])
    }

    @Test
    fun scoresOnceAndCollectsMisses() {
        val round = QuizRound(pack.questions.take(2), Random(3))
        val wrong = (round.current.right + 1) % 4
        assertFalse(round.answer(wrong))
        assertFalse(round.answer(round.current.right)) // a second tap changes nothing
        assertTrue(round.next())
        assertTrue(round.answer(round.current.right))
        assertFalse(round.next())
        assertEquals(1, round.score)
        assertEquals(listOf("q1"), round.missed.map { it.id })
    }
}
