package club.readme.android.sync

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import club.readme.android.data.DeviceMatch
import club.readme.android.data.Wallpaper
import club.readme.android.data.WallpaperPage
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder

/**
 * Wallpapers from the readme.club gallery, filtered to those that fit this reader's screen.
 * Every page, thumbnail and full file goes through a local cache that the UI reads,
 * so pages already seen stay browsable offline.
 */
class WallpaperSync(private val dir: File, private val screenWidth: Int, private val screenHeight: Int) {

    /**
     * Registry slug of this reader (see [DeviceMatch]), or null when no entry fits: the
     * gallery then shows every size. Resolved online once, then kept in the cache.
     */
    @Volatile var deviceSlug: String? = null
        private set
    @Volatile private var deviceResolved = false

    fun thumbFile(id: String) = File(dir, "thumbs/$id.jpg")

    /** Bytes used on disk by cached pages, thumbnails and full files. */
    fun sizeBytes(): Long = dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }

    fun clear() {
        dir.deleteRecursively()
    }

    /**
     * One page of the gallery, searched ([query], blank for all) and sorted ([sort]: latest,
     * popular, name or author): fresh from the site, else the cached copy, else null.
     */
    fun page(
        page: Int,
        pageSize: Int,
        query: String = "",
        sort: String = SORT_LATEST,
        scope: Scope = Scope.All,
        token: String? = null,
    ): WallpaperPage? {
        val q = query.trim()
        val cached = File(dir, "pages/${scope.key}-$sort-${q.hashCode()}-$pageSize-$page.json")
        try {
            resolveDevice()
            // A member's own lists show every size: they chose those wallpapers.
            val filter = when (scope) {
                Scope.All -> deviceSlug?.let { "fits=$it&" } ?: ""
                is Scope.Favorites -> "favorites=1&"
                is Scope.Uploads -> "member_id=${URLEncoder.encode(scope.member, "UTF-8")}&"
            }
            val search = if (q.isEmpty()) "" else "q=${URLEncoder.encode(q, "UTF-8")}&"
            // The member's own lists show everything they chose, like their count on Member.
            val sensitive = if (scope == Scope.All) "hide_sensitive=1&" else ""
            val json = String(Http.get("$SITE/api/wallpapers?${filter}${search}${sensitive}sort=$sort&page=$page&page_size=$pageSize", token))
            val parsed = parse(json)
            write(cached, json.toByteArray())
            for (w in parsed.items) {
                if (!thumbFile(w.id).exists()) downloadThumb(w)
            }
            return parsed
        } catch (e: Exception) {
            Log.w(TAG, "Wallpaper page $page failed", e)
        }
        return if (cached.exists()) parse(cached.readText()) else null
    }

    /** The full-size file (downloaded once, then from cache), or null when offline and never fetched. */
    fun full(id: String): File? {
        val file = File(dir, "full/$id")
        if (file.exists()) return file
        return try {
            // Goes through the site's download route so the gallery's download count stays right.
            write(file, Http.get("$SITE/api/wallpapers/$id/download"))
            file
        } catch (e: Exception) {
            Log.w(TAG, "Wallpaper $id download failed", e)
            null
        }
    }

    /** The submitting member's number ("#042"), cached once known; null for non-members or when offline. */
    fun memberNumber(id: String): String? {
        val file = File(dir, "members/$id")
        if (file.exists()) return file.readText().ifEmpty { null }
        return try {
            val number = JSONObject(String(Http.get("$SITE/api/wallpapers/$id/submitter"))).string("submitter_member_id")
                ?.let { if (it.startsWith("#")) it else "#$it" }
            write(file, (number ?: "").toByteArray())
            number
        } catch (e: Exception) {
            Log.w(TAG, "Submitter of $id unavailable", e)
            null
        }
    }

    private fun resolveDevice() {
        if (deviceResolved) return
        val file = File(dir, "device.txt")
        deviceSlug = try {
            val devices = JSONObject(String(Http.get("$SITE/api/devices"))).getJSONArray("devices")
            val candidates = (0 until devices.length()).map { i ->
                val d = devices.getJSONObject(i)
                DeviceMatch.Candidate(
                    slug = d.getString("slug"),
                    name = d.string("name") ?: "",
                    brand = d.string("brand") ?: "",
                    width = d.optInt("screen_width_px").takeIf { it > 0 },
                    height = d.optInt("screen_height_px").takeIf { it > 0 },
                    formatCount = d.optJSONArray("native_image_formats")?.length() ?: 0,
                )
            }
            DeviceMatch.pick(candidates, Build.MANUFACTURER, Build.MODEL, screenWidth, screenHeight)
                .also { write(file, (it ?: "").toByteArray()) }
        } catch (e: Exception) {
            Log.w(TAG, "Device registry unavailable", e)
            if (!file.exists()) return // retry on the next page
            file.readText().ifEmpty { null }
        }
        deviceResolved = true
    }

    private fun parse(json: String): WallpaperPage {
        val o = JSONObject(json)
        val items = o.getJSONArray("items")
        return WallpaperPage(
            items = (0 until items.length()).mapNotNull { i ->
                val w = items.getJSONObject(i)
                val id = w.getString("id")
                // The id names cache files and URL paths: anything but a plain token is skipped.
                if (!isSafeId(id)) return@mapNotNull null
                Wallpaper(
                    id = id,
                    title = w.string("title") ?: "Untitled",
                    author = w.string("author_name")
                        ?: w.string("reddit_username")?.let { "u/$it" }
                        ?: w.string("instagram_username")?.let { "@$it" },
                    width = w.optInt("width"),
                    height = w.optInt("height"),
                    thumbUrl = absolute(w.string("thumbnail_path") ?: "/api/wallpapers/$id/thumbnail?format=jpg"),
                )
            },
            total = o.optInt("total"),
        )
    }

    private fun downloadThumb(w: Wallpaper) {
        try {
            val bytes = Http.get(w.thumbUrl)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= THUMB_WIDTH) sample *= 2
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: error("Undecodable thumbnail")
            val file = thumbFile(w.id)
            file.parentFile?.mkdirs()
            val tmp = File(file.path + ".tmp")
            tmp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            tmp.renameTo(file)
        } catch (e: Exception) {
            Log.w(TAG, "Thumbnail skipped: ${w.thumbUrl}", e)
        }
    }

    private fun write(file: File, bytes: ByteArray) {
        file.parentFile?.mkdirs()
        val tmp = File(file.path + ".tmp")
        tmp.writeBytes(bytes)
        tmp.renameTo(file)
    }

    private fun absolute(path: String) = if (path.startsWith("/")) SITE + path else path

    /** Which wallpapers: all of them, the linked member's favourites, or one member's uploads. */
    sealed class Scope(val key: String) {
        object All : Scope("all")
        /** Keyed by member number, so another account never sees these cached pages. */
        class Favorites(member: String) : Scope(FAVORITES_PREFIX + member.filter(Char::isDigit))
        class Uploads(val member: String) : Scope("uploads-" + member.filter(Char::isDigit))
    }

    companion object {
        const val FAVORITES_PREFIX = "favorites-"
        const val SORT_LATEST = "latest"
        /** Sort orders the site accepts, in the order the pills show them. */
        val SORTS = listOf(SORT_LATEST, "popular", "name", "author")

        const val TAG = "WallpaperSync"
        const val SITE = "https://www.readme.club"
        const val THUMB_WIDTH = 240

        private val SAFE_ID = Regex("[A-Za-z0-9_-]{1,64}")

        fun isSafeId(id: String) = SAFE_ID.matches(id)
    }
}
