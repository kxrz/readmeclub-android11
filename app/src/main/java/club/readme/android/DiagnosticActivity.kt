package club.readme.android

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import club.readme.android.eink.FullRefresh
import club.readme.android.sync.ImageCache
import java.io.File

/** Hidden screen to survey the device: display metrics, button keycodes, full-refresh test. */
class DiagnosticActivity : Activity() {

    private lateinit var keys: TextView
    private val keyLog = ArrayDeque<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diagnostic)
        keys = findViewById(R.id.keys)

        val m = resources.displayMetrics
        findViewById<TextView>(R.id.display).text = buildString {
            appendLine("${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("readme.club ${BuildConfig.VERSION_NAME} · wallpapers for: ${app.wallpapers.deviceSlug ?: "all sizes"}")
            appendLine("Pixels: ${m.widthPixels}×${m.heightPixels}")
            appendLine("densityDpi: ${m.densityDpi} · density: ${m.density}")
            appendLine("xdpi/ydpi: ${m.xdpi} / ${m.ydpi}")
            appendLine("dp: ${(m.widthPixels / m.density).toInt()}×${(m.heightPixels / m.density).toInt()}")
            val cachedImages = File(filesDir, "news/img").list()?.size ?: 0
            appendLine("News images cached: $cachedImages · failed this run: ${ImageCache.failures}")
            ImageCache.lastError?.let { append("Last image error: $it") }
        }

        val flash = findViewById<View>(R.id.flash)
        findViewById<View>(R.id.refresh_test).setOnClickListener { FullRefresh.flash(flash, force = true) }
        findViewById<View>(R.id.components).setOnClickListener { startActivity(Intent(this, ComponentsActivity::class.java)) }
        findViewById<View>(R.id.back).setOnClickListener { finish() }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val action = if (event.action == KeyEvent.ACTION_DOWN) "DOWN" else "UP"
        keyLog.addFirst(
            "$action ${KeyEvent.keyCodeToString(event.keyCode)} (${event.keyCode}) " +
                "scan=${event.scanCode} repeat=${event.repeatCount} long=${event.isLongPress}"
        )
        while (keyLog.size > 10) keyLog.removeLast()
        keys.text = keyLog.joinToString("\n")
        return super.dispatchKeyEvent(event)
    }
}
