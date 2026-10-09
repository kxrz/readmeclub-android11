package club.readme.android.data

import android.content.Context

class Prefs(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val defaultTextSize = context.resources.configuration.smallestScreenWidthDp.let {
        when {
            it < 400 -> 18
            it < 600 -> 20
            else -> 22
        }
    }

    /** Force a full e-ink refresh every N page turns, or never ([REFRESH_OFF]). */
    var refreshEvery: Int
        get() = prefs.getInt("refresh_every", 6)
        set(value) = prefs.edit().putInt("refresh_every", value).apply()

    /** Reader text size, in sp. Defaults grow with the screen: 18 on pocket readers like the S4. */
    var textSize: Int
        get() = prefs.getInt("text_size", defaultTextSize)
        set(value) = prefs.edit().putInt("text_size", value).apply()

    /** Reader font: sans-serif (like the rest of the UI) unless serif is chosen. */
    var serif: Boolean
        get() = prefs.getBoolean("serif", false)
        set(value) = prefs.edit().putBoolean("serif", value).apply()

    /** Folder picked for saved wallpapers (a document tree URI), or null for Pictures/ReadmeClub. */
    var wallpaperFolder: String?
        get() = prefs.getString("wallpaper_folder", null)
        set(value) = prefs.edit().putString("wallpaper_folder", value).apply()

    /** Fewest moves to solve Lights Out (the About easter egg), 0 until a first win. */
    var lightsOutBest: Int
        get() = prefs.getInt("lights_out_best", 0)
        set(value) = prefs.edit().putInt("lights_out_best", value).apply()

    /** False until the first-launch welcome screen has been shown. */
    var welcomed: Boolean
        get() = prefs.getBoolean("welcomed", false)
        set(value) = prefs.edit().putBoolean("welcomed", value).apply()

    companion object {
        const val REFRESH_OFF = 0
        val REFRESH_CHOICES = listOf(1, 3, 6, 10, REFRESH_OFF)
        val TEXT_SIZES = listOf(16, 18, 20, 22, 24)
    }
}
