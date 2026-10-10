package club.readme.android.game

import android.app.Activity
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import club.readme.android.R
import club.readme.android.app
import club.readme.android.data.Prefs
import club.readme.android.eink.FullRefresh
import club.readme.android.eink.PageKeys

/**
 * Mines: 7 × 7 on small screens (cells stay at 44 dp or more), 10 × 10 from 480 dp wide.
 * Tap opens, long press flags; the Flag button makes a tap flag, for when holding is awkward.
 * Rounds are short, so they are not saved; best times are.
 */
class MinesActivity : Activity() {

    private lateinit var store: GameStore
    private lateinit var board: MinesView
    private lateinit var status: TextView
    private lateinit var best: TextView
    private lateinit var flagMode: TextView
    private lateinit var flash: View
    private lateinit var mines: Mines
    private var startedAt = 0L
    private var finishedMs = 0L
    private var movesSinceRefresh = 0

    // Swallow the page keys: there are no pages here.
    private val keys = PageKeys(onNext = {})

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mines)
        store = GameStore(this)
        board = findViewById(R.id.board)
        status = findViewById(R.id.status)
        best = findViewById(R.id.best)
        flagMode = findViewById(R.id.flag_mode)
        flash = findViewById(R.id.flash)
        findViewById<View>(R.id.back).setOnClickListener { finish() }
        findViewById<View>(R.id.new_game).setOnClickListener { newGame() }
        flagMode.setOnClickListener { flagMode.isSelected = !flagMode.isSelected }
        board.onTap = { if (flagMode.isSelected) flag(it) else open(it) }
        board.onLongPress = ::flag
        newGame()
    }

    private fun newGame() {
        val size = GameStore.minesSize(resources.configuration.screenWidthDp)
        mines = Mines.forSize(size)
        board.mines = mines
        flagMode.isSelected = false
        movesSinceRefresh = 0
        update()
        FullRefresh.flash(flash)
    }

    private fun open(index: Int) {
        if (!mines.started) startedAt = SystemClock.elapsedRealtime()
        mines.open(index)
        if (mines.state == Mines.State.WON) {
            finishedMs = SystemClock.elapsedRealtime() - startedAt
            store.offerBest(bestKey(), finishedMs)
        }
        moved()
    }

    private fun flag(index: Int) {
        mines.toggleFlag(index)
        moved()
    }

    private fun moved() {
        board.invalidate()
        update()
        val every = app.prefs.refreshEvery
        if (every != Prefs.REFRESH_OFF && ++movesSinceRefresh >= every * 2) {
            movesSinceRefresh = 0
            FullRefresh.flash(flash)
        }
    }

    private fun bestKey() = "mines/${mines.size}"

    private fun update() {
        val bestMs = store.best(bestKey())
        best.text = if (bestMs > 0) getString(R.string.games_best, GameStore.time(bestMs)) else ""
        status.text = when (mines.state) {
            Mines.State.WON -> getString(R.string.mines_won, GameStore.time(finishedMs))
            Mines.State.LOST -> getString(R.string.mines_lost)
            Mines.State.PLAYING -> getString(R.string.mines_flags, mines.flags, mines.mineCount)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = keys.handle(event) || super.dispatchKeyEvent(event)
}
