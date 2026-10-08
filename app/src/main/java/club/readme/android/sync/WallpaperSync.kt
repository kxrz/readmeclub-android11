package club.readme.android.sync

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import club.readme.android.data.Wallpaper
import club.readme.android.data.WallpaperPage
import org.json.JSONObject
import java.io.File

/**
 * Wallpapers from the readme.club gallery, filtered to those that fit the S4 screen.
 * Every page, thumbnail and full file goes through a local cache that the UI reads,
 * so pages already seen stay browsable offline.
 */
class WallpaperSync(private val dir: File) {

    fun thumbFile(id: String) = File(dir, "thumbs/$id.jpg")

    /** Bytes used on disk by cached pages, thumbnails and full files. */
    fun sizeBytes(): Long = dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }

    fun clear() {
        dir.deleteRecursively()
    }

    /** One page of the gallery: fresh from the site, else the cached copy, else null. */
    fun page(page: Int, pageSize: Int): WallpaperPage? {
        val cached = File(dir, "pages/$pageSize-$page.json")
        try {
            val json = String(Http.get("$SITE/api/wallpapers?fits=$FITS&hide_sensitive=1&sort=latest&page=$page&page_size=$pageSize"))
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

    private fun parse(json: String): WallpaperPage {
        val o = JSONObject(json)
        val items = o.getJSONArray("items")
        return WallpaperPage(
            items = (0 until items.length()).map { i ->
                val w = items.getJSONObject(i)
                val id = w.getString("id")
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

    private companion object {
        const val TAG = "WallpaperSync"
        const val SITE = "https://www.readme.club"
        // The site's devices registry slug: same screen ratio, formats the S4 can display.
        const val FITS = "xteink-s4"
        const val THUMB_WIDTH = 240
    }
}
