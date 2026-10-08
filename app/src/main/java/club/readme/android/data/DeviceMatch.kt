package club.readme.android.data

import kotlin.math.abs

/**
 * Picks the readme.club device registry entry that matches this reader, so the
 * wallpaper gallery only offers images that fit its screen.
 */
object DeviceMatch {

    data class Candidate(
        val slug: String,
        val name: String,
        val brand: String,
        val width: Int?,
        val height: Int?,
        /** Formats the device reads natively; on Android every common format decodes, so more is better. */
        val formatCount: Int,
    )

    /** Same tolerance as the site's wallpaper fit (src/lib/devices/fit.ts). */
    private const val RATIO_TOLERANCE = 0.02

    /**
     * In order: the entry named like this model ("S4", "Xteink S4"), else one with the exact
     * screen resolution, else one with the same aspect ratio. Ties go to the entry reading the
     * most formats. Null when nothing fits: the gallery then shows every size.
     */
    fun pick(candidates: List<Candidate>, manufacturer: String, model: String, screenWidth: Int, screenHeight: Int): String? {
        val (shortSide, longSide) = portrait(screenWidth, screenHeight)
        val withScreen = candidates.filter { it.width != null && it.height != null && it.width > 0 && it.height > 0 }

        val names = setOf(model.trim().lowercase(), "${manufacturer.trim()} ${model.trim()}".lowercase())
        candidates.firstOrNull { it.name.lowercase() in names || "${it.brand} ${it.name}".lowercase() in names }
            ?.let { return it.slug }

        val exact = withScreen.filter { portrait(it.width!!, it.height!!) == shortSide to longSide }
        if (exact.isNotEmpty()) return exact.maxByOrNull { it.formatCount }!!.slug

        val ratio = shortSide.toDouble() / longSide
        return withScreen
            .filter {
                val (s, l) = portrait(it.width!!, it.height!!)
                abs(s.toDouble() / l - ratio) / ratio <= RATIO_TOLERANCE
            }
            .maxWithOrNull(compareBy<Candidate> { it.formatCount }.thenBy { -abs(portrait(it.width!!, it.height!!).first - shortSide) })
            ?.slug
    }

    private fun portrait(w: Int, h: Int) = minOf(w, h) to maxOf(w, h)
}
