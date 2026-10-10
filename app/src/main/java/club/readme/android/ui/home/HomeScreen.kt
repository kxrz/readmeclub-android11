package club.readme.android.ui.home

import android.app.Activity
import android.text.format.DateFormat
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import club.readme.android.R
import club.readme.android.app
import club.readme.android.game.GameStore
import club.readme.android.reader.InternalLinks
import club.readme.android.reader.ReaderActivity
import java.util.Date

/**
 * Home: Continue reading, one tile per section (with its pill or sticker), and the sync
 * status with Sync in the bottom bar. Reads the local cache only.
 */
class HomeScreen(
    private val activity: Activity,
    container: ViewGroup,
    onOpen: (Section) -> Unit,
    onSyncRequested: () -> Unit,
) {

    enum class Section { NEWS, GUIDES, WALLPAPERS, GAMES, MEMBER, SETTINGS }

    private val root: View = activity.layoutInflater.inflate(R.layout.home, container, true)
    private val continueCard: View = root.findViewById(R.id.continue_card)
    private val status: TextView = root.findViewById(R.id.home_status)
    private val tiles = mapOf(
        Section.NEWS to tile(R.id.tile_news, R.string.tab_news),
        Section.GUIDES to tile(R.id.tile_guides, R.string.tab_guides),
        Section.WALLPAPERS to tile(R.id.tile_wallpapers, R.string.tab_wallpapers),
        Section.GAMES to tile(R.id.tile_games, R.string.tab_games),
        Section.MEMBER to tile(R.id.tile_member, R.string.tab_member),
        Section.SETTINGS to tile(R.id.tile_settings, R.string.tab_settings),
    )

    init {
        tiles.forEach { (section, tile) -> tile.setOnClickListener { onOpen(section) } }
        root.findViewById<View>(R.id.sync).setOnClickListener { onSyncRequested() }
        status.setOnClickListener { onSyncRequested() }
        refresh()
    }

    private fun tile(id: Int, label: Int): View =
        root.findViewById<View>(id).also { it.findViewById<TextView>(R.id.label).setText(label) }

    /** Re-reads what the home shows: last read, unread news, pending update, sync state. */
    fun refresh() {
        val app = activity.app
        val last = app.reading.lastRead
        continueCard.visibility = if (last == null) View.GONE else View.VISIBLE
        if (last != null) {
            continueCard.findViewById<TextView>(R.id.continue_title).text = last.title
            val kind = activity.getString(if (last.kind == InternalLinks.GUIDES) R.string.kind_guide else R.string.kind_news)
            continueCard.findViewById<TextView>(R.id.continue_meta).text =
                activity.getString(R.string.continue_meta, kind, last.percent)
            continueCard.setOnClickListener {
                activity.startActivity(ReaderActivity.intent(activity, last.kind, last.slug))
            }
        }

        val unread = app.reading.unreadNews.size
        badge(Section.NEWS, if (unread > 0) activity.getString(R.string.badge_new, unread) else null, sticker = true)
        val games = if (GameStore(activity).hasSudoku) R.string.badge_games_resume else R.string.badge_games
        badge(Section.GAMES, activity.getString(games), sticker = false)
        badge(Section.MEMBER, app.prefs.memberNumber, sticker = false)
        badge(Section.SETTINGS, if (app.latestRelease?.canInstall == true) activity.getString(R.string.badge_update) else null, sticker = false)
        renderStatus()
    }

    /** A sticker (one per screen: the news count) or a pill on a tile; null hides it. */
    private fun badge(section: Section, text: String?, sticker: Boolean) {
        val badge = tiles.getValue(section).findViewById<TextView>(R.id.badge)
        badge.visibility = if (text == null) View.INVISIBLE else View.VISIBLE
        badge.text = text
        if (sticker) {
            badge.setBackgroundResource(R.drawable.ds_sticker)
            badge.setTextColor(activity.getColor(R.color.ds_paper))
            badge.rotation = 3f
        }
    }

    fun renderStatus() {
        val app = activity.app
        val images = app.imageProgress
        val lastSync = app.news.lastSync
        status.text = when {
            images != null -> activity.getString(R.string.home_images, images.first, images.second)
            app.syncing -> activity.getString(R.string.home_syncing)
            app.lastSyncFailed -> activity.getString(R.string.home_offline)
            lastSync > 0 -> activity.getString(R.string.home_updated, DateFormat.getTimeFormat(activity).format(Date(lastSync)))
            else -> ""
        }
    }
}
