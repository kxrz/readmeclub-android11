package club.readme.android.sync

import club.readme.android.learn.QuizPack
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackSyncTest {

    // As the CMS REST API returns a quiz-packs document (depth 0).
    private val doc = JSONObject(
        """
        {"id": 3, "title": "E-ink & books", "slug": "e-ink-books", "description": "Screens and pages.",
         "status": "published", "questionCount": 1, "updatedAt": "2026-10-10T16:00:00.000Z",
         "questions": [{"id": "6708a1f2c3", "theme": "E-ink", "question": "Who makes most e-paper panels?",
           "answer": "E Ink", "wrong1": "Sharp", "wrong2": "LG", "wrong3": "BOE", "why": "Its film is in most readers."}]}
        """,
    )

    @Test
    fun aCmsDocumentBecomesAPlayablePack() {
        val pack = QuizPack.parse(PackSync.toPack(doc))
        assertEquals("e-ink-books", pack.id)
        assertEquals("E-ink & books", pack.title)
        assertEquals("2026-10-10T16:00:00.000Z", pack.updatedAt)
        val q = pack.questions.single()
        assertEquals("e-ink-books-6708a1f2c3", q.id)
        assertEquals(listOf("E Ink", "Sharp", "LG", "BOE"), q.answers)
        assertEquals("Its film is in most readers.", q.why)
    }

    @Test
    fun theCatalogueSkipsUnsafeSlugs() {
        val json = """{"docs": [
            {"title": "Ok", "slug": "literature", "questionCount": 40, "updatedAt": "x"},
            {"title": "Bad", "slug": "../../shared_prefs/learn", "questionCount": 10, "updatedAt": "y"},
            {"title": "No description", "slug": "geography", "description": null, "questionCount": 30, "updatedAt": "z"}]}"""
        val entries = PackSync.parseCatalogue(json)
        assertEquals(listOf("literature", "geography"), entries.map { it.slug })
        assertEquals("", entries[1].description) // JSON null is not the text "null"
        assertEquals(40, entries[0].questions)
        assertTrue(PackSync.isSafeSlug("e-ink-books"))
        assertFalse(PackSync.isSafeSlug("E-Ink"))
    }
}
