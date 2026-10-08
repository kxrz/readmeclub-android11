package club.readme.android.ui.wallpapers

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import club.readme.android.R
import club.readme.android.app
import club.readme.android.data.WallpaperPage
import club.readme.android.reader.ReaderActivity

/** Wallpapers tab: a 3 × 2 grid of wallpapers that fit the S4 screen, one page at a time. */
class WallpapersTab(private val activity: Activity, container: ViewGroup) {

    private val root: View = activity.layoutInflater.inflate(R.layout.wallpapers, container, true)
    private val status: TextView = root.findViewById(R.id.status)
    private val grid: GridLayout = root.findViewById(R.id.grid)
    private val pageLabel: TextView = root.findViewById(R.id.page_label)

    private var page = 1
    private var pageCount = 1
    private var loading = false

    init {
        status.setOnClickListener { load() }
        root.findViewById<View>(R.id.previous).setOnClickListener { turn(-1) }
        root.findViewById<View>(R.id.next).setOnClickListener { turn(1) }
        ReaderActivity.whenLaidOut(grid) { load() }
    }

    /** Next page, wrapping to the first one after the last (for the capacitive button). */
    fun nextPageWrapping() {
        page = if (page < pageCount) page + 1 else 1
        load()
    }

    private fun turn(delta: Int) {
        val target = (page + delta).coerceIn(1, pageCount)
        if (target == page) return
        page = target
        load()
    }

    private fun load() {
        if (loading) return
        loading = true
        status.setText(R.string.loading)
        val requested = page
        activity.app.io.execute {
            val result = activity.app.wallpapers.page(requested, PAGE_SIZE)
            activity.runOnUiThread {
                loading = false
                if (requested != page) return@runOnUiThread load()
                render(result)
            }
        }
    }

    private fun render(result: WallpaperPage?) {
        grid.removeAllViews()
        if (result == null) {
            status.setText(R.string.offline_retry)
            pageLabel.text = ""
            return
        }
        status.setText(R.string.wallpapers_fit)
        pageCount = maxOf(1, (result.total + PAGE_SIZE - 1) / PAGE_SIZE)
        pageLabel.text = activity.getString(R.string.page_of, page, pageCount)

        val cellWidth = grid.width / COLUMNS
        val cellHeight = grid.height / ROWS
        for (w in result.items) {
            val cell = activity.layoutInflater.inflate(R.layout.wallpaper_cell, grid, false)
            cell.layoutParams = GridLayout.LayoutParams().apply {
                width = cellWidth
                height = cellHeight
            }
            val thumb = activity.app.wallpapers.thumbFile(w.id)
            cell.findViewById<ImageView>(R.id.thumb).setImageBitmap(BitmapFactory.decodeFile(thumb.path))
            cell.findViewById<TextView>(R.id.title).text = w.title
            cell.setOnClickListener {
                activity.startActivity(
                    Intent(activity, WallpaperActivity::class.java)
                        .putExtra(WallpaperActivity.EXTRA_ID, w.id)
                        .putExtra(WallpaperActivity.EXTRA_TITLE, w.title)
                        .putExtra(WallpaperActivity.EXTRA_AUTHOR, w.author)
                )
            }
            grid.addView(cell)
        }
        if (result.items.isEmpty()) {
            grid.addView(TextView(activity).apply {
                setText(R.string.wallpapers_empty)
                gravity = Gravity.CENTER
            })
        }
    }

    private companion object {
        const val COLUMNS = 3
        const val ROWS = 2
        const val PAGE_SIZE = COLUMNS * ROWS
    }
}
