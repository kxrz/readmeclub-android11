package club.readme.android.reader

/** Finds and resolves the images referenced by CMS-rendered HTML. */
object HtmlImages {

    private const val CMS_ORIGIN = "https://write.readme.club"
    private val IMG_SRC = Regex("""<img\b[^>]*?\ssrc\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)

    /**
     * `src` values, unescaped the same way Html.fromHtml hands them to its ImageGetter,
     * so the two agree on the cache key.
     */
    fun sources(html: String): List<String> =
        IMG_SRC.findAll(html).map { it.groupValues[1].replace("&amp;", "&") }.distinct().toList()

    /** Absolute URL for a `src`; the CMS serves its media under relative `/api/media/...` paths. */
    fun resolve(src: String): String = when {
        src.startsWith("http://") || src.startsWith("https://") -> src
        src.startsWith("//") -> "https:$src"
        src.startsWith("/") -> CMS_ORIGIN + src
        else -> "$CMS_ORIGIN/$src"
    }
}
