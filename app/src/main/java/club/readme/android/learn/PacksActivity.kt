package club.readme.android.learn

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import club.readme.android.R
import club.readme.android.app
import club.readme.android.eink.FullRefresh
import club.readme.android.eink.PageKeys
import club.readme.android.sync.PackSync

/**
 * Quiz packs: the one in the APK, the downloaded ones and those the catalogue offers, three
 * per page. A pack's page plays it, downloads, updates or removes it. Installed packs work
 * offline; only the catalogue and downloads need the network.
 */
class PacksActivity : Activity() {

    private class Row(val slug: String, val title: String, val description: String, val questions: Int,
                      val installed: QuizPack?, val entry: PackSync.Entry?, val bundled: Boolean) {
        val updatable: Boolean get() = installed != null && entry != null && entry.updatedAt != installed.updatedAt
    }

    private lateinit var store: LearnStore
    private lateinit var sync: PackSync
    private var catalogue: List<PackSync.Entry>? = null
    private var loading = true
    private var busy = false
    private var rows = listOf<Row>()
    private var page = 0
    private var selected: String? = null
    private var confirmRemove = false

    private val keys = PageKeys(onNext = { turn(1) }, onPrevious = { turn(-1) })

    private val tiles by lazy { listOf(R.id.pack0, R.id.pack1, R.id.pack2).map { findViewById<View>(it) } }
    private val primary by lazy { findViewById<TextView>(R.id.primary) }
    private val status by lazy { findViewById<TextView>(R.id.status) }
    private val pages: Int get() = maxOf(1, (rows.size + PER_PAGE - 1) / PER_PAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.packs)
        store = LearnStore(this)
        sync = PackSync(store.downloadDir)
        findViewById<View>(R.id.back).setOnClickListener { if (selected != null) select(null) else finish() }
        findViewById<View>(R.id.previous).setOnClickListener { turn(-1) }
        primary.setOnClickListener { if (selected == null) turn(1) else act() }
        findViewById<View>(R.id.update).setOnClickListener { selectedRow()?.let { download(it.slug) } }
        findViewById<View>(R.id.remove).setOnClickListener { remove() }
        tiles.forEachIndexed { i, tile -> tile.setOnClickListener { rows.getOrNull(page * PER_PAGE + i)?.let { select(it.slug) } } }
        rebuild()
        render()
        app.io.execute {
            val result = sync.catalogue()
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                catalogue = result
                loading = false
                rebuild()
                render()
            }
        }
    }

    private fun rebuild() {
        val bundled = store.bundled.map { Row(it.id, it.title, it.description, it.questions.size, it, null, true) }
        val entries = catalogue.orEmpty().filter { e -> store.bundled.none { it.id == e.slug } }
        val downloaded = store.downloaded().map { p ->
            val entry = entries.firstOrNull { it.slug == p.id }
            Row(p.id, p.title, p.description, p.questions.size, p, entry, false)
        }
        val available = entries.filter { e -> downloaded.none { it.slug == e.slug } }
            .map { Row(it.slug, it.title, it.description, it.questions, null, it, false) }
        rows = bundled + downloaded + available
        page = page.coerceAtMost(pages - 1)
    }

    private fun selectedRow() = rows.firstOrNull { it.slug == selected }

    private fun select(slug: String?) {
        selected = slug
        confirmRemove = false
        findViewById<TextView>(R.id.remove).setText(R.string.packs_remove)
        render()
    }

    private fun turn(by: Int) {
        if (selected != null) return
        val target = page + by
        if (target !in 0 until pages) return
        page = target
        render()
    }

    private fun render() {
        val row = selectedRow()
        if (selected != null && row == null) selected = null
        val note = findViewById<TextView>(R.id.note)
        val previous = findViewById<View>(R.id.previous)
        findViewById<View>(R.id.list).visibility = if (row == null) View.VISIBLE else View.GONE
        findViewById<View>(R.id.detail).visibility = if (row == null) View.GONE else View.VISIBLE
        if (row == null) {
            findViewById<TextView>(R.id.title).setText(R.string.packs_title)
            note.text = when {
                loading -> getString(R.string.packs_loading)
                catalogue == null -> getString(R.string.packs_offline)
                else -> getString(R.string.packs_note)
            }
            tiles.forEachIndexed { i, tile ->
                val r = rows.getOrNull(page * PER_PAGE + i)
                tile.visibility = if (r == null) View.INVISIBLE else View.VISIBLE
                if (r != null) {
                    tile.findViewById<TextView>(R.id.name).text = r.title
                    tile.findViewById<TextView>(R.id.meta).text = meta(r)
                }
            }
            val paged = pages > 1
            previous.visibility = if (paged) View.VISIBLE else View.GONE
            previous.isEnabled = page > 0
            status.text = if (paged) getString(R.string.page_of, page + 1, pages) else ""
            primary.setText(R.string.next)
            primary.visibility = if (paged && page < pages - 1) View.VISIBLE else View.INVISIBLE
        } else {
            findViewById<TextView>(R.id.title).text = row.title
            note.text = listOf(row.description, meta(row)).filter { it.isNotBlank() }.joinToString("\n\n")
            findViewById<View>(R.id.update).visibility = if (row.updatable && !busy) View.VISIBLE else View.GONE
            findViewById<View>(R.id.remove).visibility = if (row.installed != null && !row.bundled && !busy) View.VISIBLE else View.GONE
            previous.visibility = View.GONE
            primary.setText(if (row.installed != null) R.string.packs_play else R.string.packs_download)
            primary.visibility = if (busy) View.INVISIBLE else View.VISIBLE
            status.text = if (busy) getString(R.string.packs_downloading) else ""
        }
        FullRefresh.flash(findViewById(R.id.flash))
    }

    private fun meta(r: Row): String {
        val state = when {
            r.bundled -> R.string.packs_state_bundled
            r.updatable -> R.string.packs_state_update
            r.installed != null -> R.string.packs_state_installed
            else -> R.string.packs_state_available
        }
        return getString(R.string.learn_pack_meta, r.questions) + " · " + getString(state)
    }

    private fun act() {
        val row = selectedRow() ?: return
        if (row.installed != null) startActivity(QuizActivity.intent(this, row.slug)) else download(row.slug)
    }

    private fun download(slug: String) {
        if (busy) return
        busy = true
        render()
        app.io.execute {
            val ok = sync.download(slug)
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                busy = false
                rebuild()
                render()
                if (!ok) status.setText(R.string.packs_failed)
            }
        }
    }

    /** Two taps: the first asks, the second removes the pack and everything it left behind. */
    private fun remove() {
        val row = selectedRow() ?: return
        val button = findViewById<TextView>(R.id.remove)
        if (!confirmRemove) {
            confirmRemove = true
            button.setText(R.string.packs_remove_confirm)
            return
        }
        sync.remove(row.slug)
        store.forget(row.slug)
        rebuild()
        select(null)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = keys.handle(event) || super.dispatchKeyEvent(event)

    private companion object {
        const val PER_PAGE = 3
    }
}
