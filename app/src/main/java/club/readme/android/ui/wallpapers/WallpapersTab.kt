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
import kotlin.math.roundToInt

/**
 * Wallpapers tab: a grid of wallpapers that fit this screen, one page at a time.
 * The grid follows the screen size: 3 × 2 on the S4, more cells on larger readers.
 */
class WallpapersTab(private val activity: Activity, container: ViewGroup) {

    private val root: View = activity.layoutInflater.inflate(R.layout.wallpapers, container, true)
    private val status: TextView = root.findViewById(R.id.status)
    private val grid: GridLayout = root.findViewById(R.id.grid)
    private val pageLabel: TextView = root.findViewById(R.id.page_label)

    private var columns = 3
    private var rows = 2
    private val pageSize: Int get() = columns * rows

    private var page = 1
    private var pageCount = 1
    private var loading = false

    init {
        status.setOnClickListener { load() }
        root.findViewById<View>(R.id.previous).setOnClickListener { turn(-1) }
        root.findViewById<View>(R.id.next).setOnClickListener { turn(1) }
        ReaderActivity.whenLaidOut(grid) {
            sizeGrid()
            load()
        }
    }

    private fun sizeGrid() {
        val density = activity.resources.displayMetrics.density
        columns = maxOf(2, (grid.width / density / MIN_CELL_DP).toInt())
        rows = maxOf(1, (grid.height / (grid.width / columns * CELL_RATIO)).roundToInt())
        grid.columnCount = columns
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
            val result = activity.app.wallpapers.page(requested, pageSize)
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
        status.setText(if (activity.app.wallpapers.deviceSlug != null) R.string.wallpapers_fit else R.string.wallpapers_all_sizes)
        pageCount = maxOf(1, (result.total + pageSize - 1) / pageSize)
        pageLabel.text = activity.getString(R.string.page_of, page, pageCount)

        val cellWidth = grid.width / columns
        val cellHeight = grid.height / rows
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
        /** Narrowest cell, in dp: 3 columns on the S4's 350 dp, more on wider screens. */
        const val MIN_CELL_DP = 110f
        /** Cell height / width, leaving room for the title under a portrait thumbnail. */
        const val CELL_RATIO = 1.4f
    }
}
