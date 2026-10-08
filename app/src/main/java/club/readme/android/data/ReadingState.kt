package club.readme.android.data

import android.content.Context

/**
 * What the reader has read, kept on the device only: where they stopped in each article
 * or guide, the last thing they opened, and which news articles are new to them.
 */
class ReadingState(context: Context) {

    private val prefs = context.getSharedPreferences("reading", Context.MODE_PRIVATE)

    data class LastRead(val kind: String, val slug: String, val title: String, val percent: Int)

    /** Character offset of the page they stopped on, or -1. Survives font size changes. */
    fun position(kind: String, slug: String): Int = prefs.getInt("pos/$kind/$slug", -1)

    fun savePosition(kind: String, slug: String, title: String, offset: Int, percent: Int) {
        prefs.edit()
            .putInt("pos/$kind/$slug", offset)
            .putString("last", listOf(kind, slug, percent.toString(), title).joinToString("\n"))
            .apply()
    }

    /** The last article or guide opened, unless it was read to the end. */
    val lastRead: LastRead?
        get() {
            val parts = prefs.getString("last", null)?.split("\n", limit = 4) ?: return null
            if (parts.size < 4) return null
            val percent = parts[2].toIntOrNull() ?: return null
            return if (percent >= 100) null else LastRead(parts[0], parts[1], parts[3], percent)
        }

    /** News articles that arrived after the first sync and haven't been opened yet. */
    val unreadNews: Set<String> get() = prefs.getStringSet("unread", emptySet())!!

    fun markRead(slug: String) {
        prefs.edit().putStringSet("unread", unreadNews - slug).apply()
    }

    /**
     * Called after each news sync with the slugs it returned. The very first sync only
     * remembers them, so a fresh install doesn't start with 30 "new" articles.
     */
    fun registerNews(slugs: List<String>) {
        val seen = prefs.getStringSet("seen", emptySet())!!
        val fresh = if (seen.isEmpty()) emptySet() else slugs.toSet() - seen
        prefs.edit()
            .putStringSet("seen", seen + slugs)
            .putStringSet("unread", (unreadNews + fresh).intersect(slugs.toSet()))
            .apply()
    }
}
