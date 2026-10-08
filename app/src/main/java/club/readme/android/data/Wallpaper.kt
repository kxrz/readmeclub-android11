package club.readme.android.data

data class Wallpaper(
    val id: String,
    val title: String,
    val author: String?,
    val width: Int,
    val height: Int,
    /** Absolute URL of the listing thumbnail. */
    val thumbUrl: String,
)

data class WallpaperPage(val items: List<Wallpaper>, val total: Int)
