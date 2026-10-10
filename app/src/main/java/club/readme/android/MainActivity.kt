package club.readme.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import club.readme.android.eink.FullRefresh
import club.readme.android.eink.PageKeys
import club.readme.android.game.GamesActivity
import club.readme.android.sync.WallpaperSync
import club.readme.android.ui.guides.GuidesTab
import club.readme.android.ui.home.HomeScreen
import club.readme.android.ui.home.HomeScreen.Section
import club.readme.android.ui.member.MemberTab
import club.readme.android.ui.news.NewsTab
import club.readme.android.ui.settings.SettingsTab
import club.readme.android.ui.wallpapers.WallpapersTab

/**
 * The app's single main screen: Home, or one section in its place (News, Guides,
 * Wallpapers, Settings). Each section's bottom bar starts with Back, which returns Home.
 */
class MainActivity : Activity() {

    private lateinit var content: FrameLayout
    /** Null on Home. */
    private var section: Section? = null
    private var home: HomeScreen? = null
    private var newsTab: NewsTab? = null
    private var guidesTab: GuidesTab? = null
    private var settingsTab: SettingsTab? = null
    private var memberTab: MemberTab? = null
    /** Set by Member before opening Wallpapers on the member's favourites or uploads. */
    private var wallpaperScope: Pair<WallpaperSync.Scope, Int>? = null

    // Capacitive button: next page of the open section's list (nothing on Home).
    private var nextPage: (() -> Unit)? = null
    private val keys = PageKeys(onNext = { nextPage?.invoke() })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        content = findViewById(R.id.content)
        // A fresh install has nothing to announce: only updates get the "updated" card in News.
        if (!app.prefs.welcomed) app.prefs.notesSeenVersion = NewsTab.INSTALLED_VERSION
        show(savedInstanceState?.getString(KEY_SECTION)?.let { name -> Section.values().firstOrNull { it.name == name } })
        if (savedInstanceState == null) {
            sync() // one sync and one update check per launch
            app.checkForUpdate(::markUpdate)
        }
        if (!app.prefs.welcomed) showWelcome()
    }

    /** First launch only: logo and version over the app while the first sync starts. */
    private fun showWelcome() {
        app.prefs.welcomed = true
        val splash = findViewById<View>(R.id.splash)
        findViewById<TextView>(R.id.splash_version).text =
            getString(R.string.version, BuildConfig.VERSION_NAME)
        splash.visibility = View.VISIBLE
        val hide = Runnable {
            if (splash.visibility == View.VISIBLE) {
                splash.visibility = View.GONE
                FullRefresh.flash(findViewById(R.id.flash))
            }
        }
        splash.setOnClickListener { hide.run() }
        splash.postDelayed(hide, 2500)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = keys.handle(event) || super.dispatchKeyEvent(event)

    @Deprecated("Activity.onBackPressed: still the hook on API 30–32")
    override fun onBackPressed() {
        when {
            section == null -> super.onBackPressed()
            guidesTab != null -> guidesTab?.back()
            memberTab != null -> memberTab?.back()
            wallpaperScope != null -> show(Section.MEMBER)
            else -> show(null)
        }
    }

    private fun sync() {
        app.sync(
            onText = {
                newsTab?.reload()
                guidesTab?.reload()
                home?.refresh()
            },
            onProgress = {
                newsTab?.renderProgress()
                home?.renderStatus()
            },
            done = {
                newsTab?.reload()
                guidesTab?.reload()
                settingsTab?.refresh()
                home?.refresh()
            },
        )
        newsTab?.reload() // shows the "fetching" card right away
        guidesTab?.renderStatus()
        home?.renderStatus()
    }

    override fun onResume() {
        super.onResume()
        // Back from the reader: the article is now read, and maybe left half-read.
        newsTab?.reload()
        home?.refresh()
    }

    /** The update card in News, the Update pill on Home, the update row in Settings. */
    private fun markUpdate() {
        newsTab?.reload()
        home?.refresh()
        settingsTab?.refreshUpdate()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        section?.let { outState.putString(KEY_SECTION, it.name) }
    }

    /** Home (null) or a section, in place; Games opens its own screen. */
    private fun show(target: Section?) {
        if (target == Section.GAMES) {
            startActivity(Intent(this, GamesActivity::class.java))
            return
        }
        section = target
        content.removeAllViews()
        home = null
        newsTab = null
        guidesTab = null
        settingsTab = null
        memberTab = null
        nextPage = null
        val scope = wallpaperScope.takeIf { target == Section.WALLPAPERS }
        wallpaperScope = scope
        when (target) {
            null -> home = HomeScreen(this, content, ::show, ::sync)
            Section.NEWS -> newsTab = NewsTab(this, content, ::sync).also { nextPage = it::nextPageWrapping }
            Section.GUIDES -> guidesTab = GuidesTab(this, content, ::sync) { show(null) }.also { nextPage = it::nextPageWrapping }
            Section.WALLPAPERS -> nextPage = (
                if (scope == null) WallpapersTab(this, content) else WallpapersTab(this, content, scope.first, scope.second)
            )::nextPageWrapping
            Section.MEMBER -> memberTab = MemberTab(this, content, { s, title ->
                wallpaperScope = s to title
                show(Section.WALLPAPERS)
            }) { show(null) }
            Section.SETTINGS -> settingsTab = SettingsTab(this, content, ::sync, { app.checkForUpdate(::markUpdate) }) { show(Section.MEMBER) }.also { nextPage = it::nextPageWrapping }
            Section.GAMES -> Unit
        }
        // Every section's bar starts with Back (Guides and Member handle their own steps;
        // the member's favourites and uploads go back to Member).
        if (target != null && target != Section.GUIDES && target != Section.MEMBER) {
            content.findViewById<View>(R.id.back)?.setOnClickListener { show(if (scope != null) Section.MEMBER else null) }
        }
        FullRefresh.flash(findViewById(R.id.flash))
    }

    private companion object {
        const val KEY_SECTION = "section"
    }
}
