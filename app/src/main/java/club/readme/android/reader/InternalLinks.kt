package club.readme.android.reader

/**
 * The S4 has no web browser, so the app never links out: only links to other
 * readme.club articles and guides stay active, and they open in the app's reader.
 */
object InternalLinks {

    const val NEWS = "news"
    const val GUIDES = "guides"

    data class Target(val kind: String, val slug: String)

    private val CONTENT = Regex(
        """^(?:https?://(?:www\.)?readme\.club)?/(news|guide)/([a-z0-9-]+)/?(?:[?#].*)?$""",
        RegexOption.IGNORE_CASE,
    )

    /** The article or guide [url] points to, or null if it goes anywhere else. */
    fun target(url: String): Target? {
        val match = CONTENT.find(url.trim()) ?: return null
        val kind = if (match.groupValues[1].equals("news", ignoreCase = true)) NEWS else GUIDES
        return Target(kind, match.groupValues[2].lowercase())
    }
}
