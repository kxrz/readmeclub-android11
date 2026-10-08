package club.readme.android.sync

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import java.io.File

/** Downloads images and stores them as JPEGs at most [MAX_WIDTH] px wide, ready for the e-ink screen. */
object ImageCache {

    private const val TAG = "ImageCache"
    private const val MAX_WIDTH = 480

    /** Shown on the diagnostic screen, so a failing image download can be read without adb. */
    @Volatile var failures = 0
        private set
    @Volatile var lastError: String? = null
        private set

    /** Failures are logged and counted, never fatal: content stays readable without its images. */
    fun download(url: String, target: File) {
        try {
            val bytes = Http.get(url)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= MAX_WIDTH) sample *= 2
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: error("Undecodable image (${bytes.size} bytes, ${bounds.outMimeType})")
            val width = minOf(decoded.width, MAX_WIDTH)
            val height = decoded.height * width / decoded.width
            // Flatten onto white: JPEG has no alpha, and transparent logos would otherwise turn black.
            val flat = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            Canvas(flat).apply {
                drawColor(Color.WHITE)
                drawBitmap(Bitmap.createScaledBitmap(decoded, width, height, true), 0f, 0f, null)
            }
            target.parentFile?.mkdirs()
            val tmp = File(target.path + ".tmp")
            tmp.outputStream().use { flat.compress(Bitmap.CompressFormat.JPEG, 80, it) }
            tmp.renameTo(target)
        } catch (e: Exception) {
            Log.w(TAG, "Image skipped: $url", e)
            failures++
            lastError = "$url\n$e"
        }
    }
}
