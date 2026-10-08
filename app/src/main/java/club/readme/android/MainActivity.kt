package club.readme.android

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import club.readme.android.eink.FullRefresh
import club.readme.android.eink.PageKeys
import club.readme.android.ui.guides.GuidesTab
import club.readme.android.ui.news.NewsTab
import club.readme.android.ui.settings.SettingsTab
import club.readme.android.ui.wallpapers.WallpapersTab

class MainActivity : Activity() {

    private lateinit var content: FrameLayout
    private lateinit var tabs: LinearLayout
    private var currentTab = 0
    private var newsTab: NewsTab? = null
    private var guidesTab: GuidesTab? = null
    private var settingsTab: SettingsTab? = null

    // Capacitive button: next page of the current tab's list.
    private var nextPage: (() -> Unit)? = null
    private val keys = PageKeys(onNext = { nextPage?.invoke() })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        content = findViewById(R.id.content)
        tabs = findViewById(R.id.tabs)
        for (i in 0 until tabs.childCount) {
            tabs.getChildAt(i).setOnClickListener { showTab(i) }
        }
        showTab(savedInstanceState?.getInt(KEY_TAB) ?: 0)
        if (savedInstanceState == null) {
            sync() // one sync and one update check per launch
            app.checkForUpdate(::markUpdate)
        }
        markUpdate()
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

    private fun sync() {
        app.sync {
            newsTab?.reload()
            guidesTab?.reload()
            settingsTab?.refresh()
        }
        newsTab?.reload() // shows the "fetching" card right away
        guidesTab?.renderStatus()
    }

    /** "Settings •" when an installable update is waiting there. */
    private fun markUpdate() {
        val settings = tabs.getChildAt(TAB_SETTINGS) as TextView
        val pending = app.latestRelease?.canInstall == true
        settings.text = getString(if (pending) R.string.tab_settings_update else R.string.tab_settings)
        settingsTab?.refreshUpdate()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_TAB, currentTab)
    }

    private fun showTab(index: Int) {
        currentTab = index
        for (i in 0 until tabs.childCount) {
            tabs.getChildAt(i).isSelected = i == index
        }
        content.removeAllViews()
        newsTab = null
        guidesTab = null
        settingsTab = null
        nextPage = null
        when (index) {
            TAB_NEWS -> newsTab = NewsTab(this, content, ::sync).also { nextPage = it::nextPageWrapping }
            TAB_GUIDES -> guidesTab = GuidesTab(this, content, ::sync).also { nextPage = it::nextPageWrapping }
            TAB_WALLPAPERS -> nextPage = WallpapersTab(this, content)::nextPageWrapping
            else -> settingsTab = SettingsTab(this, content, ::sync).also { nextPage = it::nextPageWrapping }
        }
    }

    private companion object {
        const val KEY_TAB = "tab"
        const val TAB_NEWS = 0
        const val TAB_GUIDES = 1
        const val TAB_WALLPAPERS = 2
        const val TAB_SETTINGS = 3
    }
}
