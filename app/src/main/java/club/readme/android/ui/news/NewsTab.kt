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

    /** Next page, wrapping to the first one after the last (for the capacitive button). */
    fun nextPageWrapping() {
        page = if (page + 1 < pageCount) page + 1 else 0
        render()
    }

    private val pageCount: Int get() = maxOf(1, (articles.size + perPage - 1) / perPage)

    private fun turn(delta: Int) {
        val target = (page + delta).coerceIn(0, pageCount - 1)
        if (target == page) return
        page = target
        render()
    }

    private fun render() {
        renderStatus()
        items.removeAllViews()
        if (articles.isEmpty()) {
            items.addView(TextView(activity).apply {
                setText(if (activity.app.syncing) R.string.loading else R.string.news_empty)
                textSize = 17f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            })
        }
        for (article in articles.drop(page * perPage).take(perPage)) {
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
