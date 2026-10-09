package club.readme.android.reader

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import club.readme.android.R
import club.readme.android.app
import club.readme.android.data.Article
import club.readme.android.data.Prefs
import club.readme.android.eink.FullRefresh
import club.readme.android.eink.PageKeys

/**
 * Paginated reader. Button or right third = next page, left third = previous,
 * middle tap, long button press or Menu in the bottom bar = menu (contents for guides, open
 * on your phone, back). The bottom bar (Back, page, Menu) is always shown: many readers have
 * no hardware button.
 * Tapping a link to another article opens it here; there are no external links (the S4
 * has no browser). Reading resumes where it stopped, and the footer shows the time left.
 */
class ReaderActivity : Activity() {

    private lateinit var pageView: PageView
    private lateinit var pageNumber: TextView
    private lateinit var menu: View
    private lateinit var contents: View
    private lateinit var contentsItems: LinearLayout
    private lateinit var qr: View
    private lateinit var flash: View
    private var turnsSinceRefresh = 0

    private lateinit var kind: String
    private var article: Article? = null
    private var pageOffsets = intArrayOf(0)
    private var wordsPerPage = intArrayOf(0)
    /** (section title, page) for each section of a guide found in the text. */
    private var sections: List<Pair<String, Int>> = emptyList()
    private var contentsPage = 0

    private val keys = PageKeys(onNext = { onNextKey() }, onPrevious = { onPreviousKey() }, onLongPress = { toggleMenu() })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reader)
        pageView = findViewById(R.id.page)
        pageNumber = findViewById(R.id.page_number)
        menu = findViewById(R.id.menu)
        contents = findViewById(R.id.contents)
        contentsItems = findViewById(R.id.contents_items)
        qr = findViewById(R.id.qr)
        flash = findViewById(R.id.flash)

        pageView.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val link = pageView.linkAt(event.x, event.y)
                when {
                    menu.visibility == View.VISIBLE -> toggleMenu()
                    link != null -> InternalLinks.target(link.url)?.let(::open)
                    event.x < view.width / 3f -> turn(-1)
                    event.x > view.width * 2 / 3f -> turn(1)
                    else -> toggleMenu()
                }
            }
            true
        }
        findViewById<View>(R.id.back).setOnClickListener { finish() }
        findViewById<View>(R.id.bar_back).setOnClickListener { finish() }
        findViewById<View>(R.id.bar_menu).setOnClickListener { toggleMenu() }
        findViewById<View>(R.id.open_contents).setOnClickListener { showContents() }
        findViewById<View>(R.id.open_qr).setOnClickListener { showQr() }
        qr.setOnClickListener { qr.visibility = View.GONE }
        findViewById<View>(R.id.contents_close).setOnClickListener { contents.visibility = View.GONE }
        findViewById<View>(R.id.contents_previous).setOnClickListener { turnContents(-1) }
        findViewById<View>(R.id.contents_next).setOnClickListener { turnContents(1) }

        val slug = intent.getStringExtra(EXTRA_SLUG) ?: return finish()
        kind = intent.getStringExtra(EXTRA_KIND) ?: InternalLinks.NEWS
        app.io.execute {
            val found = store(kind).find(slug)
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                if (found == null) return@runOnUiThread finish()
                whenLaidOut(pageView) { show(found) }
            }
        }
    }

    private fun store(kind: String) = if (kind == InternalLinks.GUIDES) app.guides else app.news

    /** Opens a linked article or guide if it is in the offline cache. */
    private fun open(target: InternalLinks.Target) {
        app.io.execute {
            val cached = store(target.kind).find(target.slug) != null
            runOnUiThread {
                if (cached) {
                    startActivity(intent(this, target.kind, target.slug))
                } else {
                    Toast.makeText(this, R.string.link_not_saved, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun show(article: Article) {
        this.article = article
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            linkColor = Color.BLACK
            typeface = if (app.prefs.serif) Typeface.SERIF else Typeface.DEFAULT
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, app.prefs.textSize.toFloat(), resources.displayMetrics)
        }
        // On large screens, cap the line length so lines stay comfortable to read.
        val maxLine = (MAX_LINE_DP * resources.displayMetrics.density).toInt()
        if (pageView.contentWidth > maxLine) {
            val side = (pageView.width - maxLine) / 2
            pageView.setPadding(side, pageView.paddingTop, side, pageView.paddingBottom)
        }
        val width = pageView.contentWidth
        val height = pageView.contentHeight
        val text = ArticleText.build(article, store(kind), resources, width, height)
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setLineSpacing(0f, 1.2f)
            .setIncludePad(false)
            .setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY)
            .setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NORMAL)
            .build()
        val tops = IntArray(layout.lineCount + 1) { layout.getLineTop(it) }
        pageView.setContent(layout, Paginator.pageStarts(tops, height))

        pageOffsets = pageView.pageOffsets
        wordsPerPage = IntArray(pageOffsets.size) { i ->
            val end = if (i + 1 < pageOffsets.size) pageOffsets[i + 1] else text.length
            ReadingMath.countWords(text, pageOffsets[i], end)
        }
        sections = article.toc.zip(ReadingMath.headingOffsets(text.toString(), article.toc))
            .filter { it.second >= 0 }
            .map { (title, offset) -> title to ReadingMath.pageForOffset(pageOffsets, offset) }
        findViewById<View>(R.id.open_contents).visibility = if (sections.isEmpty()) View.GONE else View.VISIBLE

        // Resume where the reader stopped (an offset, so a new font size still lands on the right text).
        pageView.goTo(ReadingMath.pageForOffset(pageOffsets, app.reading.position(kind, article.slug)))
        if (kind == InternalLinks.NEWS) app.reading.markRead(article.slug)
        onPageChanged()
        FullRefresh.flash(flash) // full refresh when a full-screen page opens
    }

    private fun turn(delta: Int) {
        if (menu.visibility == View.VISIBLE) return toggleMenu()
        if (!pageView.turn(delta)) return
        onPageChanged()
        val every = app.prefs.refreshEvery
        if (every != Prefs.REFRESH_OFF && ++turnsSinceRefresh >= every) {
            turnsSinceRefresh = 0
            FullRefresh.flash(flash)
        }
    }

    /** Footer and saved position follow the current page. */
    private fun onPageChanged() {
        val page = pageView.page
        pageNumber.text = getString(
            R.string.page_of_time, page + 1, pageView.pageCount, ReadingMath.minutesLeft(wordsPerPage, page),
        )
        val current = article ?: return
        val percent = (page + 1) * 100 / pageView.pageCount
        app.reading.savePosition(kind, current.slug, current.title, pageOffsets[page], percent)
    }

    private fun onNextKey() {
        when {
            qr.visibility == View.VISIBLE -> qr.visibility = View.GONE
            contents.visibility == View.VISIBLE -> turnContents(1)
            else -> turn(1)
        }
    }

    private fun onPreviousKey() {
        when {
            qr.visibility == View.VISIBLE -> qr.visibility = View.GONE
            contents.visibility == View.VISIBLE -> turnContents(-1)
            else -> turn(-1)
        }
    }

    private fun toggleMenu() {
        menu.visibility = if (menu.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun showQr() {
        val current = article ?: return
        val path = if (kind == InternalLinks.GUIDES) "guide" else "news"
        val url = "https://www.readme.club/$path/${current.slug}"
        val modulePx = (6 * resources.displayMetrics.density).toInt()
        findViewById<ImageView>(R.id.qr_image).setImageBitmap(QrBitmap.of(url, modulePx))
        findViewById<TextView>(R.id.qr_url).text = url.removePrefix("https://www.")
        menu.visibility = View.GONE
        qr.visibility = View.VISIBLE
        FullRefresh.flash(flash)
    }

    private fun showContents() {
        menu.visibility = View.GONE
        contents.visibility = View.VISIBLE
        // Start on the contents page holding the section being read.
        val current = sections.indexOfLast { it.second <= pageView.page }.coerceAtLeast(0)
        whenLaidOut(contentsItems) {
            contentsPage = current / contentsPerPage
            renderContents()
        }
    }

    private val contentsPerPage: Int
        get() = maxOf(1, contentsItems.height / (CONTENTS_ROW_DP * resources.displayMetrics.density).toInt())

    private fun turnContents(delta: Int) {
        val pages = (sections.size + contentsPerPage - 1) / contentsPerPage
        val target = (contentsPage + delta).coerceIn(0, maxOf(0, pages - 1))
        if (target == contentsPage) return
        contentsPage = target
        renderContents()
    }

    private fun renderContents() {
        contentsItems.removeAllViews()
        val rowHeight = (CONTENTS_ROW_DP * resources.displayMetrics.density).toInt()
        for ((title, page) in sections.drop(contentsPage * contentsPerPage).take(contentsPerPage)) {
            val row = layoutInflater.inflate(R.layout.contents_row, contentsItems, false) as TextView
            row.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, rowHeight)
            row.text = title
            row.isSelected = page == pageView.page
            row.setOnClickListener {
                contents.visibility = View.GONE
                pageView.goTo(page)
                onPageChanged()
                FullRefresh.flash(flash)
            }
            contentsItems.addView(row)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = keys.handle(event) || super.dispatchKeyEvent(event)

    companion object {
        private const val EXTRA_SLUG = "slug"
        private const val MAX_LINE_DP = 600
        private const val CONTENTS_ROW_DP = 52
        private const val EXTRA_KIND = "kind"

        /** Intent to read the article ([InternalLinks.NEWS]) or guide ([InternalLinks.GUIDES]) [slug]. */
        fun intent(context: android.content.Context, kind: String, slug: String): Intent =
            Intent(context, ReaderActivity::class.java).putExtra(EXTRA_KIND, kind).putExtra(EXTRA_SLUG, slug)

        /** Runs [block] once [view] has a size. */
        fun whenLaidOut(view: View, block: () -> Unit) {
            if (view.width > 0) return block()
            view.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                override fun onLayoutChange(
                    v: View, left: Int, top: Int, right: Int, bottom: Int,
                    oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int,
                ) {
                    v.removeOnLayoutChangeListener(this)
                    block()
                }
            })
        }
    }
}
