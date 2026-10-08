package club.readme.android.ui.guides

import android.app.Activity
import android.graphics.BitmapFactory
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import club.readme.android.R
import club.readme.android.app
import club.readme.android.data.Article
import club.readme.android.data.Brand
import club.readme.android.reader.InternalLinks
import club.readme.android.reader.ReaderActivity

/**
 * Guides tab, a bookshelf: first the brands, then the guides of the chosen brand.
 * Reads the local cache only; the app-wide sync fills it.
 */
class GuidesTab(private val activity: Activity, container: ViewGroup, onSyncRequested: () -> Unit) {

    private val root: View = activity.layoutInflater.inflate(R.layout.guides, container, true)
    private val title: TextView = root.findViewById(R.id.header_title)
    private val back: View = root.findViewById(R.id.back_to_brands)
    private val status: TextView = root.findViewById(R.id.status)
    private val grid: GridLayout = root.findViewById(R.id.grid)
    private val pageLabel: TextView = root.findViewById(R.id.page_label)
    private val tileHeight = activity.resources.getDimensionPixelSize(R.dimen.brand_tile_height)
    private val itemHeight = activity.resources.getDimensionPixelSize(R.dimen.news_item_height)

    private var brands: List<Brand> = emptyList()
    private var guides: List<Article> = emptyList()
    /** Null on the shelf, the open brand otherwise. */
    private var brand: Brand? = null
    private var page = 0

    init {
        status.setOnClickListener { onSyncRequested() }
        back.setOnClickListener { openBrand(null) }
        root.findViewById<View>(R.id.previous).setOnClickListener { turn(-1) }
        root.findViewById<View>(R.id.next).setOnClickListener { turn(1) }
        ReaderActivity.whenLaidOut(grid) { render() }
        reload()
    }

    fun reload() {
        val app = activity.app
        app.io.execute {
            val loadedBrands = app.guides.loadBrands()
            val loadedGuides = app.guides.load()
            activity.runOnUiThread {
                brands = loadedBrands
                guides = loadedGuides
                render()
            }
        }
    }

    /** Next page, wrapping to the first one after the last (for the capacitive button). */
    fun nextPageWrapping() {
        page = if (page + 1 < pageCount) page + 1 else 0
        render()
    }

    private fun openBrand(target: Brand?) {
        brand = target
        page = 0
        render()
    }

    private val brandGuides: List<Article> get() = guides.filter { brand!!.slug in it.brands }

    private val perPage: Int
        get() = if (brand == null) COLUMNS * maxOf(1, grid.height / tileHeight) else maxOf(1, grid.height / itemHeight)

    private val itemCount: Int get() = if (brand == null) brands.size else brandGuides.size

    private val pageCount: Int get() = maxOf(1, (itemCount + perPage - 1) / perPage)

    private fun turn(delta: Int) {
        val target = (page + delta).coerceIn(0, pageCount - 1)
        if (target == page) return
        page = target
        render()
    }

    fun renderStatus() {
        val app = activity.app
        status.text = when {
            app.syncing -> activity.getString(R.string.loading)
            app.lastSyncFailed -> activity.getString(R.string.offline_saved)
            else -> ""
        }
    }

    private fun render() {
        if (grid.width == 0) return
        renderStatus()
        grid.removeAllViews()
        page = page.coerceAtMost(pageCount - 1)
        pageLabel.text = activity.getString(R.string.page_of, page + 1, pageCount)
        val open = brand
        title.text = open?.name ?: activity.getString(R.string.tab_guides)
        back.visibility = if (open == null) View.GONE else View.VISIBLE

        if (open == null) {
            grid.columnCount = COLUMNS
            brands.drop(page * perPage).take(perPage).forEach { grid.addView(brandTile(it)) }
        } else {
            grid.columnCount = 1
            brandGuides.drop(page * perPage).take(perPage).forEach { grid.addView(guideItem(it)) }
        }
        if (itemCount == 0) {
            grid.columnCount = 1
            grid.addView(TextView(activity).apply {
                setText(if (activity.app.syncing) R.string.loading else R.string.guides_empty)
                textSize = 17f
                gravity = Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply { width = grid.width; height = grid.height }
            })
        }
    }

    private fun brandTile(b: Brand): View {
        val tile = activity.layoutInflater.inflate(R.layout.brand_tile, grid, false)
        tile.layoutParams = GridLayout.LayoutParams().apply {
            width = grid.width / COLUMNS
            height = tileHeight
        }
        val logo = b.logo?.let { BitmapFactory.decodeFile(activity.app.guides.imageFile(it).path) }
        tile.findViewById<ImageView>(R.id.logo).apply {
            setImageBitmap(logo)
            visibility = if (logo == null) View.GONE else View.VISIBLE
        }
        tile.findViewById<TextView>(R.id.name).text = b.name
        val count = guides.count { b.slug in it.brands }
        tile.findViewById<TextView>(R.id.count).text =
            activity.resources.getQuantityString(R.plurals.guide_count, count, count)
        tile.setOnClickListener { openBrand(b) }
        return tile
    }

    private fun guideItem(guide: Article): View {
        val item = activity.layoutInflater.inflate(R.layout.news_item, grid, false)
        item.layoutParams = GridLayout.LayoutParams().apply {
            width = grid.width
            height = itemHeight
        }
        item.findViewById<TextView>(R.id.title).apply {
            text = guide.title
            ellipsize = TextUtils.TruncateAt.END
        }
        item.findViewById<TextView>(R.id.meta).text = guide.meta
        item.setOnClickListener {
            activity.startActivity(ReaderActivity.intent(activity, InternalLinks.GUIDES, guide.slug))
        }
        return item
    }

    private companion object {
        const val COLUMNS = 2
    }
}
