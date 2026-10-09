package club.readme.android.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Html
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import club.readme.android.R
import club.readme.android.app
import club.readme.android.eink.FullRefresh
import club.readme.android.eink.PageKeys
import club.readme.android.reader.PageView
import club.readme.android.reader.Paginator
import club.readme.android.reader.ReaderActivity

/** Release notes (a CHANGELOG.md excerpt), paginated like the reader: Back · Previous · n / N · Next. */
class NotesActivity : Activity() {

    private lateinit var pageView: PageView
    private lateinit var pageLabel: TextView

    private val keys = PageKeys(onNext = { turn(1) }, onPrevious = { turn(-1) })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notes)
        pageView = findViewById(R.id.page)
        pageLabel = findViewById(R.id.page_label)
        findViewById<TextView>(R.id.title).text = intent.getStringExtra(EXTRA_TITLE)
        findViewById<View>(R.id.back).setOnClickListener { finish() }
        findViewById<View>(R.id.previous).setOnClickListener { turn(-1) }
        findViewById<View>(R.id.next).setOnClickListener { turn(1) }
        val markdown = intent.getStringExtra(EXTRA_MARKDOWN) ?: getString(R.string.notes_missing)
        ReaderActivity.whenLaidOut(pageView) { show(markdown) }
    }

    private fun show(markdown: String) {
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = if (app.prefs.serif) Typeface.SERIF else Typeface.DEFAULT
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, app.prefs.textSize.toFloat(), resources.displayMetrics)
        }
        val text = Html.fromHtml(ReleaseNotes.toHtml(markdown), Html.FROM_HTML_MODE_COMPACT)
        val width = pageView.contentWidth
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setLineSpacing(0f, 1.2f)
            .setIncludePad(false)
            .build()
        pageView.setContent(layout, Paginator.pageStarts(IntArray(layout.lineCount + 1) { layout.getLineTop(it) }, pageView.contentHeight))
        update()
        FullRefresh.flash(findViewById(R.id.flash))
    }

    private fun turn(delta: Int) {
        if (pageView.turn(delta)) update()
    }

    private fun update() {
        pageLabel.text = getString(R.string.page_of, pageView.page + 1, pageView.pageCount)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = keys.handle(event) || super.dispatchKeyEvent(event)

    companion object {
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_MARKDOWN = "markdown"

        fun intent(context: Context, title: String, markdown: String?): Intent =
            Intent(context, NotesActivity::class.java).putExtra(EXTRA_TITLE, title).putExtra(EXTRA_MARKDOWN, markdown)

        /** CHANGELOG.md as shipped in the APK (see app/build.gradle.kts). */
        fun bundledChangelog(context: Context): String =
            context.assets.open("CHANGELOG.md").bufferedReader().use { it.readText() }
    }
}
