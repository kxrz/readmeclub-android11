package club.readme.android

import android.app.Application
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import club.readme.android.data.ContentStore
import club.readme.android.data.Prefs
import club.readme.android.data.ReadingState
import club.readme.android.reader.HtmlImages
import club.readme.android.sync.GuideSync
import club.readme.android.sync.ImageCache
import club.readme.android.sync.MemberSync
import club.readme.android.sync.NewsSync
import club.readme.android.sync.WallpaperSync
import club.readme.android.update.Updater
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** App container: the few shared objects, created once. */
class App : Application() {

    lateinit var news: ContentStore
        private set
    lateinit var guides: ContentStore
        private set
    lateinit var prefs: Prefs
        private set
    lateinit var wallpapers: WallpaperSync
        private set
    lateinit var member: MemberSync
        private set
    lateinit var reading: ReadingState
        private set
    val io: ExecutorService = Executors.newCachedThreadPool()
    private val main = Handler(Looper.getMainLooper())

    var syncing = false
        private set
    var lastSyncFailed = false
        private set

    /** Latest release seen by the last update check (null until one succeeds, or if the last one failed). */
    var latestRelease: Updater.Release? = null

    override fun onCreate() {
        super.onCreate()
        news = ContentStore(File(filesDir, "news"))
        guides = ContentStore(File(filesDir, "guides"))
        prefs = Prefs(this)
        reading = ReadingState(this)
        // Physical size of the built-in screen, in its natural orientation.
        val mode = getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY).mode
        wallpapers = WallpaperSync(File(cacheDir, "wallpapers"), mode.physicalWidth, mode.physicalHeight)
        member = MemberSync(prefs, cacheDir, "${mode.physicalWidth}x${mode.physicalHeight}")
    }

    /** Checks GitHub for a newer release in the background; [done] is called on the main thread. */
    fun checkForUpdate(done: () -> Unit) {
        io.execute {
            val release = Updater.latest()
            main.post {
                latestRelease = release
                done()
            }
        }
    }

    /** (done, total) while the sync downloads images, null otherwise. */
    @Volatile var imageProgress: Pair<Int, Int>? = null
        private set

    /**
     * Syncs news then guides in the background, text first so both are readable within
     * seconds, then all missing images in parallel. On the main thread: [onText] after each
     * text step, [onProgress] every few images, [done] at the end.
     */
    fun sync(onText: () -> Unit, onProgress: () -> Unit, done: () -> Unit) {
        if (syncing) return
        syncing = true
        io.execute {
            val newsImages = NewsSync(news).fetch()
            if (newsImages != null) reading.registerNews(news.load().map { it.slug })
            main.post { onText() }
            val guideImages = GuideSync(guides).fetch()
            main.post { onText() }

            val jobs = newsImages.orEmpty().map { news.imageFile(it) to it } +
                guideImages.orEmpty().map { guides.imageFile(it) to it }
            val missing = jobs.filterNot { it.first.exists() }.map { (file, src) -> HtmlImages.resolve(src) to file }
            ImageCache.downloadAll(missing) { count, total ->
                imageProgress = count to total
                if (count % PROGRESS_STEP == 0 || count == total) main.post { onProgress() }
            }
            // Drop images no current article or guide uses (only when the list itself was refreshed).
            if (newsImages != null) news.pruneImages(newsImages.map(news::imageFile).toSet())
            if (guideImages != null) guides.pruneImages(guideImages.map(guides::imageFile).toSet())

            main.post {
                syncing = false
                imageProgress = null
                lastSyncFailed = newsImages == null || guideImages == null
                done()
            }
        }
    }

    private companion object {
        /** Repaint the progress line every N images: each repaint costs an e-ink refresh. */
        const val PROGRESS_STEP = 5
    }
}

val Context.app: App get() = applicationContext as App
