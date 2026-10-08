package club.readme.android.sync

import android.util.Log
import club.readme.android.data.Article
import club.readme.android.data.ContentStore
import club.readme.android.reader.HtmlImages
import org.json.JSONObject
import java.io.File

/** Pulls the latest published articles from the CMS into [ContentStore], images included. */
class NewsSync(private val store: ContentStore) {

    /** Returns false if the CMS could not be reached; the existing cache is then left untouched. */
    fun run(): Boolean {
        val articles = try {
            parse(String(Http.get(LIST_URL)))
        } catch (e: Exception) {
            Log.w(TAG, "News sync failed", e)
            return false
        }

        val images = mutableSetOf<File>()
        for (article in articles) {
            for (src in listOfNotNull(article.heroImage) + HtmlImages.sources(article.html)) {
                val file = store.imageFile(src)
                images += file
                if (!file.exists()) ImageCache.download(HtmlImages.resolve(src), file)
            }
        }
        store.save(articles)
        store.pruneImages(images)
        return true
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
