package club.readme.android

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import club.readme.android.eink.FullRefresh
import club.readme.android.eink.PageKeys
import club.readme.android.game.LightsOutActivity
import club.readme.android.reader.QrBitmap

/**
 * About, on four pages (no scrolling): readme.club, Help (a QR code to the contact form,
 * filled in with this reader's details), the member account, and the app itself.
 */
class AboutActivity : Activity() {

    private lateinit var pages: List<View>
    private lateinit var pageLabel: TextView
    private var page = 0

    private val keys = PageKeys(onNext = { turn(1) }, onPrevious = { turn(-1) })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about)
        pages = listOf(R.id.page_club, R.id.page_help, R.id.page_member, R.id.page_app).map { findViewById(it) }
        pageLabel = findViewById(R.id.page_label)
        findViewById<View>(R.id.back).setOnClickListener { finish() }
        findViewById<View>(R.id.about_logo).setOnLongClickListener {
            startActivity(Intent(this, LightsOutActivity::class.java))
            true
        }
        findViewById<View>(R.id.previous).setOnClickListener { turn(-1) }
        findViewById<View>(R.id.next).setOnClickListener { turn(1) }
        bindHelp()
        bindMember()
        findViewById<TextView>(R.id.app_version).text = BuildConfig.VERSION_NAME
        show(savedInstanceState?.getInt(KEY_PAGE) ?: 0)
    }

    /** The details a report needs, shown and sent with the contact form. */
    private fun bindHelp() {
        val screen = resources.displayMetrics.let { "${it.widthPixels} × ${it.heightPixels}" }
        val reader = "${Build.MANUFACTURER} ${Build.MODEL}"
        findViewById<TextView>(R.id.reader_details).text = getString(
            R.string.about_reader_details, reader, Build.VERSION.RELEASE, BuildConfig.VERSION_NAME, screen,
        )
        val message = getString(R.string.about_report_template, reader, Build.VERSION.RELEASE, BuildConfig.VERSION_NAME, screen)
        val url = Uri.parse(CONTACT_URL).buildUpon()
            .appendQueryParameter("subject", "Bug report")
            .appendQueryParameter("message", message)
            .build().toString()
        findViewById<ImageView>(R.id.help_qr).setImageBitmap(QrBitmap.of(url, 4))
    }

    private fun bindMember() {
        val number = app.prefs.memberNumber
        findViewById<TextView>(R.id.member_title).text =
            if (number != null) getString(R.string.about_member_linked, number) else getString(R.string.about_account_title)
        findViewById<TextView>(R.id.member_body).setText(
            if (number != null) R.string.about_member_linked_body else R.string.about_account_body,
        )
        // Already a member here: no need to join.
        if (number != null) {
            findViewById<View>(R.id.join_qr).visibility = View.GONE
            findViewById<View>(R.id.join_url).visibility = View.GONE
        }
    }

    private fun turn(delta: Int) {
        val target = (page + delta).coerceIn(0, pages.size - 1)
        if (target != page) show(target)
    }

    private fun show(index: Int) {
        page = index.coerceIn(0, pages.size - 1)
        pages.forEachIndexed { i, v -> v.visibility = if (i == page) View.VISIBLE else View.GONE }
        pageLabel.text = getString(R.string.page_of, page + 1, pages.size)
        findViewById<View>(R.id.previous).visibility = if (page > 0) View.VISIBLE else View.INVISIBLE
        // Next never wraps: none on the last page.
        findViewById<View>(R.id.next).visibility = if (page < pages.size - 1) View.VISIBLE else View.INVISIBLE
        FullRefresh.flash(findViewById(R.id.flash))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PAGE, page)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = keys.handle(event) || super.dispatchKeyEvent(event)

    private companion object {
        const val KEY_PAGE = "page"
        const val CONTACT_URL = "https://www.readme.club/contact"
    }
}
