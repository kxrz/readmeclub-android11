package club.readme.android.ui.settings

import android.app.Activity
import android.content.Intent
import android.os.SystemClock
import android.text.format.DateFormat
import android.text.format.Formatter
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import club.readme.android.AboutActivity
import club.readme.android.BuildConfig
import club.readme.android.DiagnosticActivity
import club.readme.android.R
import club.readme.android.app
import club.readme.android.data.Prefs
import club.readme.android.update.Updater
import java.util.Date

/** Settings, on three pages (no scrolling): Reading, Sync & storage, About. */
class SettingsTab(private val activity: Activity, container: ViewGroup, private val onSyncRequested: () -> Unit) {

    private val root: View = activity.layoutInflater.inflate(R.layout.settings, container, true)
    private val pages = listOf<View>(
        root.findViewById(R.id.page_reading),
        root.findViewById(R.id.page_storage),
        root.findViewById(R.id.page_about),
    )
    private val pageLabel: TextView = root.findViewById(R.id.page_label)
    private val lastSync: TextView = root.findViewById(R.id.last_sync)
    private val updateStatus: TextView = root.findViewById(R.id.manifest_status)
    private val updateNow: TextView = root.findViewById(R.id.update_now)
    private var page = 0

    // Hidden diagnostic screen: 5 taps on the version line within 3 s.
    private var versionTaps = 0
    private var firstTapAt = 0L

    init {
        val prefs = activity.app.prefs
        choices(R.id.text_size_choices, Prefs.TEXT_SIZES.map { it.toString() to it }, prefs.textSize) { prefs.textSize = it }
        choices(
            R.id.font_choices,
            listOf(activity.getString(R.string.font_sans) to false, activity.getString(R.string.font_serif) to true),
            prefs.serif,
        ) { prefs.serif = it }
        choices(
            R.id.refresh_choices,
            Prefs.REFRESH_CHOICES.map { (if (it == Prefs.REFRESH_OFF) activity.getString(R.string.off) else it.toString()) to it },
            prefs.refreshEvery,
        ) { prefs.refreshEvery = it }

        root.findViewById<View>(R.id.sync_now).setOnClickListener { onSyncRequested(); refresh() }
        clearButton(R.id.clear_news) { activity.app.news.clear() }
        clearButton(R.id.clear_guides) { activity.app.guides.clear() }
        clearButton(R.id.clear_wallpapers) { activity.app.wallpapers.clear() }

        bindAbout()
        root.findViewById<View>(R.id.previous).setOnClickListener { show(maxOf(0, page - 1)) }
        root.findViewById<View>(R.id.next).setOnClickListener { show(minOf(pages.size - 1, page + 1)) }
        show(0)
        refresh()
    }

    /** Next page, wrapping to the first one after the last (for the capacitive button). */
    fun nextPageWrapping() = show((page + 1) % pages.size)

    /** Re-reads sync time and storage sizes (after a sync or a clear). */
    fun refresh() {
        val app = activity.app
        val time = app.news.lastSync
        lastSync.text = when {
            app.syncing -> activity.getString(R.string.loading)
            time > 0 -> activity.getString(R.string.last_sync, DateFormat.getTimeFormat(activity).format(Date(time)))
            else -> activity.getString(R.string.never_synced)
        }
        app.io.execute {
            val sizes = listOf(app.news.sizeBytes(), app.guides.sizeBytes(), app.wallpapers.sizeBytes())
            activity.runOnUiThread {
                listOf(R.id.size_news, R.id.size_guides, R.id.size_wallpapers).zip(sizes).forEach { (id, bytes) ->
                    root.findViewById<TextView>(id).text = Formatter.formatShortFileSize(activity, bytes)
                }
            }
        }
    }

    private fun show(index: Int) {
        page = index
        pages.forEachIndexed { i, v -> v.visibility = if (i == index) View.VISIBLE else View.GONE }
        pageLabel.text = activity.getString(R.string.page_of, index + 1, pages.size)
    }

    /** A row of mutually exclusive, inverted-when-selected choices. */
    private fun <T> choices(rowId: Int, options: List<Pair<String, T>>, current: T, onPick: (T) -> Unit) {
        val row = root.findViewById<LinearLayout>(rowId)
        for ((label, value) in options) {
            val choice = activity.layoutInflater.inflate(R.layout.choice, row, false) as TextView
            choice.text = label
            choice.isSelected = value == current
            choice.setOnClickListener {
                onPick(value)
                for (i in 0 until row.childCount) row.getChildAt(i).isSelected = row.getChildAt(i) === choice
            }
            row.addView(choice)
        }
    }

    private fun clearButton(id: Int, clear: () -> Unit) {
        root.findViewById<View>(id).setOnClickListener {
            // Clearing under a running sync would race with its writes.
            if (activity.app.syncing) return@setOnClickListener
            activity.app.io.execute {
                clear()
                activity.runOnUiThread { refresh() }
            }
        }
    }

    private fun bindAbout() {
        root.findViewById<View>(R.id.about).setOnClickListener {
            activity.startActivity(Intent(activity, AboutActivity::class.java))
        }
        val version = root.findViewById<TextView>(R.id.version)
        version.text = activity.getString(R.string.version, BuildConfig.VERSION_NAME)
        version.setOnClickListener {
            val now = SystemClock.uptimeMillis()
            if (now - firstTapAt > 3000) {
                firstTapAt = now
                versionTaps = 0
            }
            if (++versionTaps == 5) {
                versionTaps = 0
                activity.startActivity(Intent(activity, DiagnosticActivity::class.java))
            }
        }

        updateNow.setOnClickListener { update() }
        refreshUpdate()
    }

    /** Shows the result of the launch-time update check (see MainActivity). */
    fun refreshUpdate() {
        val release = activity.app.latestRelease
        updateStatus.text = when {
            release == null -> activity.getString(R.string.offline)
            release.isNewer -> activity.getString(R.string.update_available, release.version)
            else -> activity.getString(R.string.up_to_date, release.version)
        }
        updateNow.visibility = if (release?.canInstall == true) View.VISIBLE else View.GONE
        if (release != null) updateNow.text = activity.getString(R.string.update_now, release.version)
    }

    private fun update() {
        val release = activity.app.latestRelease ?: return
        if (!Updater.ensureInstallAllowed(activity)) {
            updateStatus.setText(R.string.allow_installs)
            return
        }
        updateNow.isEnabled = false
        updateStatus.setText(R.string.downloading)
        activity.app.io.execute {
            val result = runCatching { Updater.install(activity, Updater.download(activity, release)) }
            activity.runOnUiThread {
                updateNow.isEnabled = true
                result.exceptionOrNull()?.let { updateStatus.text = activity.getString(R.string.update_failed, it.message) }
            }
        }
    }
}
