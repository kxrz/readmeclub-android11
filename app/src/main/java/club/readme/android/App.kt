package club.readme.android

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import club.readme.android.data.ContentStore
import club.readme.android.data.Prefs
import club.readme.android.sync.GuideSync
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
    val io: ExecutorService = Executors.newCachedThreadPool()
    private val main = Handler(Looper.getMainLooper())

    var syncing = false
        private set
    var lastSyncFailed = false
        private set

    /** Latest release seen by the last update check (null until one succeeds). */
    var latestRelease: Updater.Release? = null

    override fun onCreate() {
        super.onCreate()
        news = ContentStore(File(filesDir, "news"))
        guides = ContentStore(File(filesDir, "guides"))
        prefs = Prefs(this)
        wallpapers = WallpaperSync(File(cacheDir, "wallpapers"))
    }

    /** Checks GitHub for a newer release in the background; [done] is called on the main thread. */
    fun checkForUpdate(done: () -> Unit) {
        io.execute {
            val release = Updater.latest()
            main.post {
                if (release != null) latestRelease = release
                done()
            }
        }
    }

    /** Syncs news and guides in the background; [done] is called on the main thread. */
    fun sync(done: () -> Unit) {
        if (syncing) return
        syncing = true
        io.execute {
            val ok = NewsSync(news).run() and GuideSync(guides).run()
            main.post {
                syncing = false
                lastSyncFailed = !ok
                done()
            }
        }
    }
}

val Context.app: App get() = applicationContext as App
