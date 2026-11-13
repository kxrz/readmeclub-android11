package club.readme.android.reader

/**
 * Rewrites the CMS's article and guide HTML into what Html.fromHtml lays out well on e-ink.
 * The CMS renders images as `<figure>`s with a `srcset` and an optional `<figcaption>`, and
 * embeds videos as `<iframe>`s; Html.fromHtml knows none of these tags.
 *
 * - An image takes the smallest `srcset` size that still fills [forReader]'s `imageWidth`
 *   (the image cache scales everything down to it anyway), not the original.
 * - A caption becomes a small line under its image.
 * - A video, which cannot play here, becomes a line saying where it is and how to watch it.
 *
 * Runs at sync, before the HTML is saved, so the reader and the image cache read the same `src`.
 */
object CmsHtml {

    private val OPTIONS = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    private val EMBED_FIGURE = Regex("""<figure\b[^>]*\bclass\s*=\s*["'][^"']*\bembed\b[^"']*["'][^>]*>(.*?)</figure>""", OPTIONS)
    private val IFRAME = Regex("""<iframe\b[^>]*>(?:.*?</iframe>)?""", OPTIONS)
    private val IFRAME_SRC = Regex("""<iframe\b[^>]*?\ssrc\s*=\s*["']([^"']*)["']""", OPTIONS)
    private val CAPTION = Regex("""<figcaption\b[^>]*>(.*?)</figcaption>""", OPTIONS)
    private val FIGURE_OPEN = Regex("""<figure\b[^>]*>""", OPTIONS)
    private val FIGURE_CLOSE = Regex("""</figure\s*>""", RegexOption.IGNORE_CASE)
    private val IMG = Regex("""<img\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val SRCSET = Regex("""\ssrcset\s*=\s*(["'])(.*?)\1""", OPTIONS)
    private val SIZES = Regex("""\ssizes\s*=\s*(["']).*?\1""", OPTIONS)
    private val SRC = Regex("""(\ssrc\s*=\s*)(["'])(.*?)\2""", OPTIONS)
    private val CANDIDATE = Regex("""^(\S+)\s+(\d+)w$""")
    private val TAGS = Regex("""<[^>]+>""")

    fun forReader(html: String, imageWidth: Int): String = html
        .replace(EMBED_FIGURE) { videoLine(it.groupValues[1]) }
        .replace(IFRAME) { videoLine(it.value) }
        .replace(CAPTION) { "<p><small><i>${it.groupValues[1].trim()}</i></small></p>" }
        .replace(FIGURE_OPEN, "<div>")
        .replace(FIGURE_CLOSE, "</div>")
        .replace(IMG) { fittedImage(it.value, imageWidth) }

    /** The `src` of the smallest `srcset` candidate at least [width] wide (else the widest), without `srcset` and `sizes`. */
    private fun fittedImage(tag: String, width: Int): String {
        val srcset = SRCSET.find(tag) ?: return tag
        val candidates = srcset.groupValues[2].split(Regex(""",\s+""")).mapNotNull { part ->
            CANDIDATE.find(part.trim())?.let { it.groupValues[1] to it.groupValues[2].toInt() }
        }
        val stripped = tag.replace(SRCSET, "").replace(SIZES, "")
        val best = candidates.filter { it.second >= width }.minByOrNull { it.second }
            ?: candidates.maxByOrNull { it.second }
            ?: return stripped
        val src = SRC.find(stripped) ?: return "<img src=\"${best.first}\"" + stripped.substring(4)
        val quote = src.groupValues[2]
        return stripped.replaceRange(src.range, "${src.groupValues[1]}$quote${best.first}$quote")
    }

    /** "Video (YouTube): caption. To watch it: Menu › Open on your phone." */
    private fun videoLine(fragment: String): String {
        val provider = IFRAME_SRC.find(fragment)?.groupValues?.get(1)?.let(::provider)
        val caption = CAPTION.find(fragment)?.groupValues?.get(1)?.trim()?.takeIf { TAGS.replace(it, "").isNotBlank() }
        val what = "<b>Video</b>" + (provider?.let { " ($it)" } ?: "") + (caption?.let { ": $it" } ?: "")
        val stop = if (caption != null && TAGS.replace(caption, "").trimEnd().last() in ".!?") "" else "."
        return "<p>$what$stop <i>To watch it: Menu › Open on your phone.</i></p>"
    }

    private fun provider(src: String): String? {
        val host = Regex("""^(?:https?:)?//([^/?#]+)""", RegexOption.IGNORE_CASE).find(src)
            ?.groupValues?.get(1)?.lowercase()?.removePrefix("www.") ?: return null
        return when {
            host == "youtu.be" || host.endsWith("youtube.com") || host.endsWith("youtube-nocookie.com") -> "YouTube"
            host.endsWith("vimeo.com") -> "Vimeo"
            host.endsWith("tiktok.com") -> "TikTok"
            else -> null
        }
    }
}
