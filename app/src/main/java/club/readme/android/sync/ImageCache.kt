package club.readme.android.sync

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Downloads images and stores them as JPEGs at most [MAX_WIDTH] px wide, ready for the e-ink screen. */
object ImageCache {

    private const val TAG = "ImageCache"
    private const val MAX_WIDTH = 480
    /** Enough to keep Wi-Fi busy without starving a small reader's CPU and memory. */
    private const val PARALLEL = 4

    /** Shown on the diagnostic screen, so a failing image download can be read without adb. */
    @Volatile var failures = 0
        private set
    @Volatile var lastError: String? = null
        private set

    /**
     * Downloads [jobs] (url → target file) a few at a time, calling [onProgress] with
     * (done, total) after each one, from a background thread. Returns when all are done.
     */
    fun downloadAll(jobs: List<Pair<String, File>>, onProgress: (Int, Int) -> Unit) {
        if (jobs.isEmpty()) return
        val pool = Executors.newFixedThreadPool(PARALLEL)
        val done = AtomicInteger()
        for ((url, target) in jobs) {
            pool.execute {
                download(url, target)
                onProgress(done.incrementAndGet(), jobs.size)
            }
        }
        pool.shutdown()
        pool.awaitTermination(30, TimeUnit.MINUTES)
    }

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
