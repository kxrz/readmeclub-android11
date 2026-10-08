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
import android.widget.TextView
import android.widget.Toast
import club.readme.android.R
import club.readme.android.app
import club.readme.android.data.Article
import club.readme.android.eink.FullRefresh
import club.readme.android.eink.PageKeys

/**
 * Paginated reader. Button or right third = next page, left third = previous,
 * middle tap or long button press = menu (back). Tapping a link to another
 * article opens it here; there are no external links (the S4 has no browser).
 */
class ReaderActivity : Activity() {

    private lateinit var pageView: PageView
    private lateinit var pageNumber: TextView
    private lateinit var menu: View
    private lateinit var flash: View
    private var turnsSinceRefresh = 0

    private val keys = PageKeys(onNext = { turn(1) }, onPrevious = { turn(-1) }, onLongPress = { toggleMenu() })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reader)
        pageView = findViewById(R.id.page)
        pageNumber = findViewById(R.id.page_number)
        menu = findViewById(R.id.menu)
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

        val slug = intent.getStringExtra(EXTRA_SLUG) ?: return finish()
        val kind = intent.getStringExtra(EXTRA_KIND) ?: InternalLinks.NEWS
        app.io.execute {
            val found = store(kind).find(slug)
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                if (found == null) return@runOnUiThread finish()
                whenLaidOut(pageView) { show(found, kind) }
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

    private fun show(article: Article, kind: String) {
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            linkColor = Color.BLACK
            typeface = if (app.prefs.serif) Typeface.SERIF else Typeface.DEFAULT
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, app.prefs.textSize.toFloat(), resources.displayMetrics)
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
        updatePageNumber()
        FullRefresh.flash(flash) // full refresh when a full-screen page opens
    }

    private fun turn(delta: Int) {
        if (menu.visibility == View.VISIBLE) return toggleMenu()
        if (!pageView.turn(delta)) return
        updatePageNumber()
        if (++turnsSinceRefresh >= app.prefs.refreshEvery) {
            turnsSinceRefresh = 0
            FullRefresh.flash(flash)
        }
    }

    private fun updatePageNumber() {
        pageNumber.text = getString(R.string.page_of, pageView.page + 1, pageView.pageCount)
    }

    private fun toggleMenu() {
        menu.visibility = if (menu.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = keys.handle(event) || super.dispatchKeyEvent(event)

    companion object {
        private const val EXTRA_SLUG = "slug"
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
