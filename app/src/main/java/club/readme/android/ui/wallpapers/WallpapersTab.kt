package club.readme.android.ui.wallpapers

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import club.readme.android.R
import club.readme.android.app
import club.readme.android.data.WallpaperPage
import club.readme.android.reader.ReaderActivity
import club.readme.android.sync.WallpaperSync
import kotlin.math.roundToInt

/**
 * Wallpapers: a grid of wallpapers that fit this screen, one page at a time, with a search
 * field and four sort orders. The grid follows the screen size: 3 columns on the S4, more
 * cells on larger readers.
 */
class WallpapersTab(
    private val activity: Activity,
    container: ViewGroup,
    /** All wallpapers, or the linked member's favourites or uploads (from Member). */
    private val scope: WallpaperSync.Scope = WallpaperSync.Scope.All,
    title: Int = R.string.tab_wallpapers,
) {

    private val root: View = activity.layoutInflater.inflate(R.layout.wallpapers, container, true)
    private val status: TextView = root.findViewById(R.id.status)
    private val grid: GridLayout = root.findViewById(R.id.grid)
    private val pageLabel: TextView = root.findViewById(R.id.page_label)

    private var columns = 3
    private var rows = 2
    private val pageSize: Int get() = columns * rows

    private val search: EditText = root.findViewById(R.id.search)
    private val clear: View = root.findViewById(R.id.clear)
    private val sorts: ViewGroup = root.findViewById(R.id.sorts)

    private var page = 1
    private var pageCount = 1
    private var loading = false
    private var query = ""
    private var sort = WallpaperSync.SORT_LATEST

    init {
        root.findViewById<TextView>(R.id.title).setText(title)
        // A member's own lists are short: no search, sorting is enough.
        if (scope != WallpaperSync.Scope.All) root.findViewById<View>(R.id.search_row).visibility = View.GONE
        status.setOnClickListener { load() }
        search.setOnEditorActionListener { _, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return@setOnEditorActionListener false
            runSearch(search.text.toString())
            true
        }
        clear.setOnClickListener {
            search.setText("")
            runSearch("")
        }
        for (i in 0 until sorts.childCount) {
            sorts.getChildAt(i).setOnClickListener { sortBy(WallpaperSync.SORTS[i]) }
        }
        showSort()
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

    private fun runSearch(text: String) {
        query = text.trim()
        clear.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
        activity.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(search.windowToken, 0)
        search.clearFocus()
        page = 1
        load()
    }

    private fun sortBy(order: String) {
        if (order == sort) return
        sort = order
        showSort()
        page = 1
        load()
    }

    /** The active sort is inverted, like a selected setting. */
    private fun showSort() {
        for (i in 0 until sorts.childCount) sorts.getChildAt(i).isSelected = WallpaperSync.SORTS[i] == sort
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
        val requested = Triple(page, query, sort)
        activity.app.io.execute {
            val result = activity.app.wallpapers.page(
                requested.first, pageSize, requested.second, requested.third, scope,
                // The token goes only where it is needed: the member's favourites.
                activity.app.prefs.memberToken.takeIf { scope == WallpaperSync.Scope.Favorites },
            )
            activity.runOnUiThread {
                loading = false
                if (requested != Triple(page, query, sort)) return@runOnUiThread load()
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
        status.setText(
            when {
                scope != WallpaperSync.Scope.All -> R.string.wallpapers_all_sizes
                activity.app.wallpapers.deviceSlug != null -> R.string.wallpapers_fit
                else -> R.string.wallpapers_all_sizes
            },
        )
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
