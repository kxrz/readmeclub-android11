package club.readme.android.sync

import android.util.Log
import club.readme.android.learn.QuizPack
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Quiz packs from the CMS (`quiz-packs`, published only): the catalogue, and downloads into
 * [dir] in the app's own pack format (see [QuizPack]), one `<slug>.json` per pack.
 */
class PackSync(private val dir: File) {

    class Entry(val slug: String, val title: String, val description: String, val questions: Int, val updatedAt: String)

    /** Published packs, or null when the CMS can't be reached. */
    fun catalogue(): List<Entry>? = try {
        parseCatalogue(String(Http.get(CATALOGUE_URL)))
    } catch (e: Exception) {
        Log.w(TAG, "Pack catalogue unavailable", e)
        null
    }

    /** Downloads [slug] and stores it; false when offline, unpublished or unreadable. */
    fun download(slug: String): Boolean {
        if (!isSafeSlug(slug)) return false
        return try {
            val docs = JSONObject(String(Http.get(PACK_URL + slug))).getJSONArray("docs")
            if (docs.length() == 0) return false
            val json = toPack(docs.getJSONObject(0))
            QuizPack.parse(json) // refuse anything the app couldn't play
            dir.mkdirs()
            val tmp = File(dir, "$slug.json.tmp")
            tmp.writeText(json)
            tmp.renameTo(File(dir, "$slug.json"))
        } catch (e: Exception) {
            Log.w(TAG, "Pack $slug download failed", e)
            false
        }
    }

    fun remove(slug: String) {
        if (isSafeSlug(slug)) File(dir, "$slug.json").delete()
    }

    companion object {
        private const val TAG = "PackSync"
        private const val API = "https://write.readme.club/api/quiz-packs"
        private const val CATALOGUE_URL = "$API?select%5Btitle%5D=true&select%5Bslug%5D=true" +
            "&select%5Bdescription%5D=true&select%5BquestionCount%5D=true&select%5BupdatedAt%5D=true" +
            "&sort=title&limit=100&depth=0"
        private const val PACK_URL = "$API?limit=1&depth=0&where%5Bslug%5D%5Bequals%5D="

        private val SAFE_SLUG = Regex("[a-z0-9-]{1,60}")

        /** The slug names a file: plain lowercase tokens only. */
        fun isSafeSlug(slug: String) = SAFE_SLUG.matches(slug)

        fun parseCatalogue(json: String): List<Entry> {
            val docs = JSONObject(json).getJSONArray("docs")
            return (0 until docs.length()).mapNotNull { i ->
                val o = docs.getJSONObject(i)
                val slug = o.optString("slug")
                if (!isSafeSlug(slug)) return@mapNotNull null
                Entry(slug, o.getString("title"), o.optString("description"), o.optInt("questionCount"), o.optString("updatedAt"))
            }
        }

        /** A CMS document as an app pack: question ids are "<slug>-<row id>", so they stay stable across edits. */
        fun toPack(doc: JSONObject): String {
            val slug = doc.getString("slug")
            val rows = doc.getJSONArray("questions")
            val questions = JSONArray()
            for (i in 0 until rows.length()) {
                val r = rows.getJSONObject(i)
                questions.put(
                    JSONObject()
                        .put("id", "$slug-${r.getString("id")}")
                        .put("theme", r.getString("theme"))
                        .put("q", r.getString("question"))
                        .put("a", JSONArray(listOf(r.getString("answer"), r.getString("wrong1"), r.getString("wrong2"), r.getString("wrong3"))))
                        .put("why", r.getString("why")),
                )
            }
            return JSONObject()
                .put("id", slug)
                .put("title", doc.getString("title"))
                .put("description", doc.optString("description"))
                .put("updatedAt", doc.optString("updatedAt"))
                .put("questions", questions)
                .toString()
        }
    }
}
