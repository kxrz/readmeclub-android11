package club.readme.android.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class Article(
    val slug: String,
    val title: String,
    val category: String?,
    val author: String?,
    /** ISO 8601, as sent by the CMS. */
    val publishedAt: String,
    val html: String,
    /** Header image `src` (also its cache key), or null. */
    val heroImage: String? = null,
    /** Brand slugs (guides only). */
    val brands: List<String> = emptyList(),
    /** Section titles in reading order (guides only), for the contents. */
    val toc: List<String> = emptyList(),
) {
    /** "Reviews · Florent · Oct 1, 2026", skipping missing parts. */
    val meta: String
        get() = listOfNotNull(category, author, formatDate(publishedAt)).joinToString(" · ")

    private companion object {
        val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)

        fun formatDate(iso: String): String? = try {
            Instant.parse(iso).atZone(ZoneId.systemDefault()).format(DATE)
        } catch (e: Exception) {
            null
        }
    }
}
