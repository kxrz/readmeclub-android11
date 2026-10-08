package club.readme.android.ui.news

import android.app.Activity
import android.text.TextUtils
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import club.readme.android.R
import club.readme.android.app
import club.readme.android.data.Article
import club.readme.android.reader.InternalLinks
import club.readme.android.reader.ReaderActivity
import java.util.Date

/** News tab: cached articles, one screen-sized page of the list at a time. */
class NewsTab(private val activity: Activity, container: ViewGroup, onSyncRequested: () -> Unit) {

    private val root: View = activity.layoutInflater.inflate(R.layout.news, container, true)
    private val status: TextView = root.findViewById(R.id.status)
    private val items: LinearLayout = root.findViewById(R.id.items)
    private val pageLabel: TextView = root.findViewById(R.id.page_label)
    private val itemHeight = activity.resources.getDimensionPixelSize(R.dimen.news_item_height)

    private var articles: List<Article> = emptyList()
    private var page = 0
    /** Body of the "fetching" card when it is on screen, updated in place with the image progress. */
    private var syncCardBody: TextView? = null
    private var perPage = 1

    init {
        status.setOnClickListener { onSyncRequested() }
        root.findViewById<View>(R.id.previous).setOnClickListener { turn(-1) }
        root.findViewById<View>(R.id.next).setOnClickListener { turn(1) }
        ReaderActivity.whenLaidOut(items) {
            perPage = maxOf(1, items.height / itemHeight)
            render()
        }
        reload()
    }

    /** Re-reads the local cache (the only source the UI reads). */
    fun reload() {
        val app = activity.app
        app.io.execute {
            val loaded = app.news.load()
            activity.runOnUiThread {
                articles = loaded
                page = page.coerceAtMost(pageCount - 1)
                render()
            }
        }
    }

    /** Updates the "fetching" card with the image download progress, without redrawing the list. */
    fun renderProgress() {
        val body = syncCardBody ?: return
        val progress = activity.app.imageProgress
        body.text = if (progress == null) {
            activity.getString(R.string.sync_card_body)
        } else {
            activity.getString(R.string.sync_card_images, progress.first, progress.second)
        }
    }

    /** Next page, wrapping to the first one after the last (for the capacitive button). */
    fun nextPageWrapping() {
        page = if (page + 1 < pageCount) page + 1 else 0
        render()
    }

    /** The list as shown: a "fetching" card first while a sync runs (null), then the articles. */
    private val entries: List<Article?>
        get() = if (activity.app.syncing) listOf(null) + articles else articles

    private val pageCount: Int get() = maxOf(1, (entries.size + perPage - 1) / perPage)

    private fun turn(delta: Int) {
        val target = (page + delta).coerceIn(0, pageCount - 1)
        if (target == page) return
        page = target
        render()
    }

    private fun render() {
        renderStatus()
        items.removeAllViews()
        syncCardBody = null
        if (entries.isEmpty()) {
            items.addView(TextView(activity).apply {
                setText(R.string.news_empty)
                textSize = 17f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            })
        }
        for (article in entries.drop(page * perPage).take(perPage)) {
            if (article == null) {
                val card = activity.layoutInflater.inflate(R.layout.sync_card, items, false)
                syncCardBody = card.findViewById(R.id.sync_card_body)
                renderProgress()
                items.addView(card)
                continue
            }
            val item = activity.layoutInflater.inflate(R.layout.news_item, items, false)
            item.findViewById<TextView>(R.id.title).apply {
                text = article.title
                ellipsize = TextUtils.TruncateAt.END
            }
            item.findViewById<TextView>(R.id.meta).text = article.meta
            item.setOnClickListener {
                activity.startActivity(ReaderActivity.intent(activity, InternalLinks.NEWS, article.slug))
            }
            items.addView(item)
        }
        pageLabel.text = activity.getString(R.string.page_of, page + 1, pageCount)
    }

    fun renderStatus() {
        val app = activity.app
        val lastSync = app.news.lastSync
        status.text = when {
            app.syncing -> activity.getString(R.string.loading)
            app.lastSyncFailed -> activity.getString(R.string.offline_saved)
            lastSync > 0 -> activity.getString(R.string.updated_at, DateFormat.getTimeFormat(activity).format(Date(lastSync)))
            else -> ""
        }
    }
}
