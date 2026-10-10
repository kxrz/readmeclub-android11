package club.readme.android.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The pack in the APK: well formed, and every screen of it fits on the S4. */
class QuizPackTest {

    private val pack = QuizPack.parse(File("src/main/assets/packs/general.json").readText())

    @Test
    fun generalKnowledgeHas200Questions() {
        assertEquals("general", pack.id)
        assertEquals(200, pack.questions.size)
        assertEquals(200, pack.questions.map { it.id }.toSet().size)
    }

    @Test
    fun everyQuestionFitsTheLimits() {
        for (q in pack.questions) {
            assertTrue(q.id, q.text.length <= QuizPack.MAX_QUESTION && q.text.endsWith("?"))
            assertTrue(q.id, q.answers.all { it.isNotBlank() && it.length <= QuizPack.MAX_ANSWER })
            assertEquals(q.id, 4, q.answers.toSet().size)
            assertTrue(q.id, q.why.isNotBlank() && q.why.length <= QuizPack.MAX_WHY)
            assertTrue(q.id, q.theme.isNotBlank())
        }
    }
}
