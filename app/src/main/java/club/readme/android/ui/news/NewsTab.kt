package club.readme.android.ui.news

import android.app.Activity
import android.graphics.Typeface
import android.text.TextUtils
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import club.readme.android.BuildConfig
import club.readme.android.R
import club.readme.android.app
import club.readme.android.data.Article
import club.readme.android.data.ReadingState
import club.readme.android.reader.InternalLinks
import club.readme.android.reader.ReaderActivity
import club.readme.android.update.NotesActivity
import club.readme.android.update.ReleaseNotes
import club.readme.android.update.Updater
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

    /** Marks the "fetching" card's slot in [entries]. */
    private object SyncCard

    /** Marks the "updated to x.y.z" card, shown once after an update. */
    private object UpdatedCard

    /**
     * The list as shown: a "fetching" card while a sync runs, an "update available" card (from
     * the launch-time check) or an "updated" card, a "continue reading" card when something was
     * left half-read, then the articles.
     */
    private val entries: List<Any>
        get() = buildList {
            if (activity.app.syncing) add(SyncCard)
            activity.app.latestRelease?.takeIf { it.isNewer }?.let(::add)
            if (activity.app.prefs.notesSeenVersion != INSTALLED_VERSION) add(UpdatedCard)
            activity.app.reading.lastRead?.let(::add)
            addAll(articles)
        }

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
        val unread = activity.app.reading.unreadNews
        for (entry in entries.drop(page * perPage).take(perPage)) {
            when (entry) {
                SyncCard -> {
                    val card = activity.layoutInflater.inflate(R.layout.sync_card, items, false)
                    syncCardBody = card.findViewById(R.id.sync_card_body)
                    renderProgress()
                    items.addView(card)
                }
                is Updater.Release -> items.addView(
                    card(activity.getString(R.string.update_card_title, entry.version), activity.getString(R.string.update_card_body)) {
                        activity.startActivity(
                            NotesActivity.intent(activity, activity.getString(R.string.whats_new_in, entry.version), entry.notes)
                        )
                    }
                )
                UpdatedCard -> items.addView(
                    card(activity.getString(R.string.updated_card_title, INSTALLED_VERSION), activity.getString(R.string.updated_card_body)) {
                        activity.app.prefs.notesSeenVersion = INSTALLED_VERSION
                        val notes = ReleaseNotes.section(NotesActivity.bundledChangelog(activity), INSTALLED_VERSION)
                        activity.startActivity(
                            NotesActivity.intent(activity, activity.getString(R.string.whats_new_in, INSTALLED_VERSION), notes)
                        )
                    }
                )
                is ReadingState.LastRead -> items.addView(
                    item(
                        title = entry.title,
                        meta = activity.getString(R.string.continue_reading_meta, entry.percent),
                        bold = true,
                        boxed = true,
                    ) { activity.startActivity(ReaderActivity.intent(activity, entry.kind, entry.slug)) }
                )
                is Article -> items.addView(
                    // Unread news stand out in bold: hierarchy by weight, never by colour.
                    item(entry.title, entry.meta, bold = entry.slug in unread, boxed = false) {
                        activity.startActivity(ReaderActivity.intent(activity, InternalLinks.NEWS, entry.slug))
                    }
                )
            }
        }
        pageLabel.text = activity.getString(R.string.page_of, page + 1, pageCount)
    }

    private fun card(title: String, body: String, onClick: () -> Unit): View {
        val card = activity.layoutInflater.inflate(R.layout.sync_card, items, false)
        card.findViewById<TextView>(R.id.sync_card_title).text = title
        card.findViewById<TextView>(R.id.sync_card_body).text = body
        card.setOnClickListener { onClick() }
        return card
    }

    private fun item(title: String, meta: String, bold: Boolean, boxed: Boolean, onClick: () -> Unit): View {
        val item = activity.layoutInflater.inflate(R.layout.news_item, items, false)
        if (boxed) item.setBackgroundResource(R.drawable.brand_tile_bg)
        item.findViewById<TextView>(R.id.title).apply {
            text = title
            ellipsize = TextUtils.TruncateAt.END
            setTypeface(null, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
        item.findViewById<TextView>(R.id.meta).text = meta
        item.setOnClickListener { onClick() }
        return item
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

    companion object {
        /** "1.0.6" for both the release and the debug build ("1.0.6-debug"). */
        val INSTALLED_VERSION: String = BuildConfig.VERSION_NAME.substringBefore('-')
    }
}
