package club.readme.android.sync

import android.util.Log
import club.readme.android.data.Article
import club.readme.android.data.ContentStore
import club.readme.android.reader.HtmlImages
import org.json.JSONObject

/** Pulls the latest published articles from the CMS into [ContentStore]. Images are fetched afterwards, see App.sync. */
class NewsSync(private val store: ContentStore) {

    /**
     * Saves the articles' text right away, so the list shows up before any image is downloaded,
     * and returns the image `src`s they need. Null if the CMS could not be reached: the existing
     * cache is then left untouched.
     */
    fun fetch(): List<String>? {
        val articles = try {
            parse(String(Http.get(LIST_URL)))
        } catch (e: Exception) {
            Log.w(TAG, "News sync failed", e)
            return null
        }
        store.save(articles)
        return articles.flatMap { listOfNotNull(it.heroImage) + HtmlImages.sources(it.html) }.distinct()
    }

    private fun parse(json: String): List<Article> {
        val docs = JSONObject(json).getJSONArray("docs")
        return (0 until docs.length()).map { i ->
            val o = docs.getJSONObject(i)
            Article(
                slug = o.getString("slug"),
                title = o.getString("title"),
                category = o.firstName("categories"),
                author = o.firstName("authors"),
                // publishedAt is set once at first publication; createdAt is the stable fallback (same as the site).
                publishedAt = o.string("publishedAt") ?: o.string("createdAt") ?: "",
                html = o.string("contentHTML") ?: "",
                // The 900 px "card" rendition (we downscale to 480 anyway), else the original.
                heroImage = o.mediaUrl("heroImage", "card"),
            )
        }.sortedByDescending { it.publishedAt }
    }

    private companion object {
        const val TAG = "NewsSync"
        const val LIST_URL = "https://write.readme.club/api/articles" +
            "?where%5B_status%5D%5Bequals%5D=published&sort=-publishedAt&depth=1&limit=30"
    }
}
