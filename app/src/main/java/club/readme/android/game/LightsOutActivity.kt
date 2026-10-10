package club.readme.android.game

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import club.readme.android.R
import club.readme.android.app
import club.readme.android.data.Prefs
import club.readme.android.eink.FullRefresh
import club.readme.android.eink.PageKeys

/**
 * Lights Out, in Games › Play (and still behind a long press on the logo in About): a puzzle
 * an e-reader without a frontlight is perfectly suited to. Turn-based, one redraw per move.
 */
class LightsOutActivity : Activity() {

    private val game = LightsOut()
    private lateinit var grid: LightsView
    private lateinit var label: TextView
    private lateinit var flash: View
    private var movesSinceRefresh = 0

    // Swallow the page keys: there are no pages here.
    private val keys = PageKeys(onNext = {})

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.lights_out)
        grid = findViewById(R.id.grid)
        label = findViewById(R.id.moves)
        flash = findViewById(R.id.flash)
        findViewById<View>(R.id.back).setOnClickListener { finish() }
        findViewById<View>(R.id.new_game).setOnClickListener { newGame() }
        grid.onPress = ::press
        newGame()
    }

    private fun newGame() {
        game.shuffle()
        grid.game = game
        movesSinceRefresh = 0
        update()
        FullRefresh.flash(flash)
    }

    private fun press(row: Int, col: Int) {
        if (game.solved) return
        game.press(row, col)
        grid.invalidate()
        if (game.solved) {
            val best = app.prefs.lightsOutBest
            if (best == 0 || game.moves < best) app.prefs.lightsOutBest = game.moves
        }
        update()
        val every = app.prefs.refreshEvery
        if (every != Prefs.REFRESH_OFF && ++movesSinceRefresh >= every * 2) {
            movesSinceRefresh = 0
            FullRefresh.flash(flash)
        }
    }

    private fun update() {
        val best = app.prefs.lightsOutBest
        label.text = when {
            game.solved -> getString(R.string.lights_solved, game.moves, best)
            else -> resources.getQuantityString(R.plurals.lights_moves, game.moves, game.moves)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = keys.handle(event) || super.dispatchKeyEvent(event)
}
