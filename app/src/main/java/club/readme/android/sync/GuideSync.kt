package club.readme.android.sync

import android.util.Log
import club.readme.android.data.Article
import club.readme.android.data.Brand
import club.readme.android.data.ContentStore
import club.readme.android.reader.HtmlImages
import org.json.JSONArray
import org.json.JSONObject

/** Pulls every published guide and the brands they belong to into [ContentStore]. Images are fetched afterwards, see App.sync. */
class GuideSync(private val store: ContentStore) {

    /**
     * Saves guides and brands right away and returns the image `src`s they need (logos,
     * header and step images). Null if the CMS could not be reached: the cache is left untouched.
     */
    fun fetch(): List<String>? {
        val (guides, brands) = try {
            parseGuides(String(Http.get(GUIDES_URL))) to parseBrands(String(Http.get(BRANDS_URL)))
        } catch (e: Exception) {
            Log.w(TAG, "Guide sync failed", e)
            return null
        }
        // Only brands that have at least one guide make it to the shelf.
        val shelf = brands.filter { b -> guides.any { b.slug in it.brands } }
        store.save(guides)
        store.saveBrands(shelf)
        return (shelf.mapNotNull { it.logo } + guides.flatMap { listOfNotNull(it.heroImage) + HtmlImages.sources(it.html) }).distinct()
    }

    private fun parseGuides(json: String): List<Article> {
        val docs = JSONObject(json).getJSONArray("docs")
        return (0 until docs.length()).map { i ->
            val o = docs.getJSONObject(i)
            Article(
                slug = o.getString("slug"),
                title = o.getString("title"),
                // Shown where articles show their category: "Beginner · 15 min".
                category = listOfNotNull(
                    o.string("difficulty")?.replaceFirstChar { it.uppercase() },
                    o.optInt("estimatedTime").takeIf { it > 0 }?.let { "$it min" },
                ).joinToString(" · ").ifEmpty { null },
                author = o.firstName("authors"),
                publishedAt = o.string("lastUpdatedAt") ?: o.string("publishedAt") ?: o.string("createdAt") ?: "",
                html = o.string("contentHTML") ?: "",
                heroImage = o.mediaUrl("heroImage", "card"),
                brands = slugs(o.optJSONArray("brands")),
            )
        }.sortedBy { it.title.lowercase() }
    }

    private fun parseBrands(json: String): List<Brand> {
        val docs = JSONObject(json).getJSONArray("docs")
        return (0 until docs.length()).map { i ->
            val o = docs.getJSONObject(i)
            Brand(slug = o.getString("slug"), name = o.getString("name"), logo = o.mediaUrl("logo", "thumbnail"))
        }.sortedBy { it.name.lowercase() }
    }

    private fun slugs(array: JSONArray?): List<String> =
        (0 until (array?.length() ?: 0)).mapNotNull { array!!.optJSONObject(it)?.string("slug") }

    private companion object {
        const val TAG = "GuideSync"
        const val GUIDES_URL = "https://write.readme.club/api/guides" +
            "?where%5B_status%5D%5Bequals%5D=published&depth=1&limit=200"
        const val BRANDS_URL = "https://write.readme.club/api/brands?depth=1&limit=100"
    }
}
