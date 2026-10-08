package club.readme.android.data

import android.content.Context

class Prefs(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Force a full e-ink refresh every N page turns. */
    var refreshEvery: Int
        get() = prefs.getInt("refresh_every", 6)
        set(value) = prefs.edit().putInt("refresh_every", value).apply()

    /** Reader text size, in sp. */
    var textSize: Int
        get() = prefs.getInt("text_size", 18)
        set(value) = prefs.edit().putInt("text_size", value).apply()

    /** Reader font: sans-serif (like the rest of the UI) unless serif is chosen. */
    var serif: Boolean
        get() = prefs.getBoolean("serif", false)
        set(value) = prefs.edit().putBoolean("serif", value).apply()

    /** False until the first-launch welcome screen has been shown. */
    var welcomed: Boolean
        get() = prefs.getBoolean("welcomed", false)
        set(value) = prefs.edit().putBoolean("welcomed", value).apply()

    companion object {
        val REFRESH_CHOICES = listOf(1, 3, 6, 10)
        val TEXT_SIZES = listOf(16, 18, 20, 22, 24)
    }
}
