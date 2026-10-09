package club.readme.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import club.readme.android.eink.FullRefresh
import club.readme.android.eink.PageKeys
import club.readme.android.game.LightsOutActivity

/** About readme.club, on two screens (no scrolling): the promise and thanks, then the member account. */
class AboutActivity : Activity() {

    private lateinit var pages: List<View>
    private lateinit var pageLabel: TextView
    private var page = 0

    private val keys = PageKeys(onNext = { turn(1) }, onPrevious = { turn(-1) })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about)
        pages = listOf(findViewById(R.id.page_promise), findViewById(R.id.page_account))
        pageLabel = findViewById(R.id.page_label)
        findViewById<View>(R.id.back).setOnClickListener { finish() }
        findViewById<View>(R.id.about_logo).setOnLongClickListener {
            startActivity(Intent(this, LightsOutActivity::class.java))
            true
        }
        findViewById<View>(R.id.previous).setOnClickListener { turn(-1) }
        findViewById<View>(R.id.next).setOnClickListener { turn(1) }
        show(0)
    }

    private fun turn(delta: Int) {
        val target = (page + delta).coerceIn(0, pages.size - 1)
        if (target != page) show(target)
    }

    private fun show(index: Int) {
        page = index
        pages.forEachIndexed { i, v -> v.visibility = if (i == index) View.VISIBLE else View.GONE }
        pageLabel.text = getString(R.string.page_of, index + 1, pages.size)
        FullRefresh.flash(findViewById(R.id.flash))
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = keys.handle(event) || super.dispatchKeyEvent(event)
}
