package club.readme.android.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * Local cache of articles or guides: one JSON file plus resized images.
 * The UI only reads from here; the `sync` package is the only writer.
 */
class ContentStore(private val dir: File) {

    private val file = File(dir, "articles.json")
    private val imageDir = File(dir, "img")

    /** Time of the last successful sync, or 0 if never synced. */
    val lastSync: Long get() = file.lastModified()

    fun load(): List<Article> {
        if (!file.exists()) return emptyList()
        val array = JSONArray(file.readText())
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Article(
                slug = o.getString("slug"),
                title = o.getString("title"),
                category = o.optString("category").ifEmpty { null },
                author = o.optString("author").ifEmpty { null },
                publishedAt = o.getString("publishedAt"),
                html = o.getString("html"),
                heroImage = o.optString("heroImage").ifEmpty { null },
                brands = o.optJSONArray("brands")?.let { b -> (0 until b.length()).map(b::getString) } ?: emptyList(),
            )
        }
    }

    fun find(slug: String): Article? = load().firstOrNull { it.slug == slug }

    fun save(articles: List<Article>) {
        val array = JSONArray()
        for (a in articles) {
            array.put(
                JSONObject()
                    .put("slug", a.slug)
                    .put("title", a.title)
                    .put("category", a.category ?: "")
                    .put("author", a.author ?: "")
                    .put("publishedAt", a.publishedAt)
                    .put("html", a.html)
                    .put("heroImage", a.heroImage ?: "")
                    .put("brands", JSONArray(a.brands))
            )
        }
        write(file, array.toString())
    }

    fun loadBrands(): List<Brand> {
        val brandsFile = File(dir, "brands.json")
        if (!brandsFile.exists()) return emptyList()
        val array = JSONArray(brandsFile.readText())
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Brand(o.getString("slug"), o.getString("name"), o.optString("logo").ifEmpty { null })
        }
    }

    fun saveBrands(brands: List<Brand>) {
        val array = JSONArray()
        for (b in brands) array.put(JSONObject().put("slug", b.slug).put("name", b.name).put("logo", b.logo ?: ""))
        write(File(dir, "brands.json"), array.toString())
    }

    private fun write(target: File, text: String) {
        dir.mkdirs()
        val tmp = File(target.path + ".tmp")
        tmp.writeText(text)
        tmp.renameTo(target)
    }

    /** Cached file for an image, keyed by its `src` exactly as written in the HTML. */
    fun imageFile(src: String): File {
        val hash = MessageDigest.getInstance("SHA-1").digest(src.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(imageDir, "$hash.jpg")
    }

    /** Bytes used on disk by this cache. */
    fun sizeBytes(): Long = dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }

    /** Empties the cache; the next sync fills it again. */
    fun clear() {
        dir.deleteRecursively()
    }

    /** Deletes cached images that no current article uses. */
    fun pruneImages(keep: Set<File>) {
        imageDir.listFiles()?.filter { it !in keep }?.forEach { it.delete() }
    }
}
