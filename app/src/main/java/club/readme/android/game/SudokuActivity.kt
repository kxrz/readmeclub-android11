package club.readme.android.game

import android.app.Activity
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import club.readme.android.R
import club.readme.android.app
import club.readme.android.data.Prefs
import club.readme.android.eink.FullRefresh
import club.readme.android.eink.PageKeys

/**
 * Sudoku: 6 × 6 on small screens (cells stay at 44 dp or more), 9 × 9 from 440 dp wide.
 * Tap a cell, then a digit on the pad. The game in progress is saved when leaving.
 */
class SudokuActivity : Activity() {

    private lateinit var store: GameStore
    private lateinit var board: SudokuView
    private lateinit var status: TextView
    private lateinit var best: TextView
    private lateinit var flash: View
    private val levelButtons = mutableMapOf<Sudoku.Level, TextView>()

    private var sudoku: Sudoku? = null
    private var level = Sudoku.Level.EASY
    private var elapsedMs = 0L
    private var resumedAt = 0L
    private var movesSinceRefresh = 0
    /** The level a second tap would start, after a first tap on a game in progress. */
    private var confirmNew: Sudoku.Level? = null

    // Swallow the page keys: there are no pages here.
    private val keys = PageKeys(onNext = {})

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sudoku)
        store = GameStore(this)
        board = findViewById(R.id.board)
        status = findViewById(R.id.status)
        best = findViewById(R.id.best)
        flash = findViewById(R.id.flash)
        findViewById<View>(R.id.back).setOnClickListener { finish() }
        findViewById<View>(R.id.new_game).setOnClickListener { askNewGame(level) }
        board.onSelect = { index ->
            val s = sudoku
            board.selected = if (s == null || s.given[index] || s.solved) -1 else index
        }

        val saved = store.loadSudoku()
        val size = saved?.sudoku?.size ?: sizeForScreen()
        buildLevels()
        buildPad(size)
        if (saved != null) {
            level = saved.level
            elapsedMs = saved.elapsedMs
            show(saved.sudoku)
        } else {
            newGame(level)
        }
    }

    private fun sizeForScreen() = GameStore.sudokuSize(resources.configuration.screenWidthDp)

    private fun buildLevels() {
        val row = findViewById<LinearLayout>(R.id.levels)
        val names = mapOf(
            Sudoku.Level.EASY to R.string.sudoku_easy,
            Sudoku.Level.MEDIUM to R.string.sudoku_medium,
            Sudoku.Level.HARD to R.string.sudoku_hard,
        )
        for ((l, name) in names) {
            val button = button(getString(name)) { if (l != level || sudoku?.solved == true) askNewGame(l) }
            row.addView(button, weighted(last = l == Sudoku.Level.HARD))
            levelButtons[l] = button
        }
    }

    /** Digits and Erase, in two rows: 1–4 / 5 6 Erase on 6 × 6, 1–5 / 6–9 Erase on 9 × 9. */
    private fun buildPad(size: Int) {
        val pad = findViewById<LinearLayout>(R.id.pad)
        pad.removeAllViews()
        val perRow = if (size == 6) 4 else 5
        val keys = (1..size).map { it.toString() to it } + (getString(R.string.sudoku_erase) to 0)
        keys.chunked(perRow).forEachIndexed { r, chunk ->
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            chunk.forEachIndexed { i, (label, digit) ->
                val lp = weighted(last = i == chunk.lastIndex)
                // Erase takes the room the missing digits leave.
                if (digit == 0) lp.weight = (perRow - chunk.size + 1).toFloat()
                row.addView(button(label) { enter(digit) }, lp)
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            if (r > 0) lp.topMargin = dp(8)
            pad.addView(row, lp)
        }
    }

    private fun button(label: String, onClick: () -> Unit) =
        TextView(this, null, 0, R.style.Ds_Button).apply {
            text = label
            setOnClickListener { onClick() }
        }

    private fun weighted(last: Boolean) =
        LinearLayout.LayoutParams(0, resources.getDimensionPixelSize(R.dimen.ds_touch), 1f).apply {
            if (!last) rightMargin = dp(8)
        }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    /**
     * A new grid at [l]. A game with the player's own digits is only replaced on a second tap
     * (a brushed button on e-ink must not throw away 20 minutes); nothing happens while a
     * grid is being made.
     */
    private fun askNewGame(l: Sudoku.Level) {
        val s = sudoku ?: return
        val progress = !s.solved && s.cells.indices.any { s.cells[it] != 0 && !s.given[it] }
        if (progress && confirmNew != l) {
            confirmNew = l
            status.setText(R.string.sudoku_confirm_new)
            return
        }
        confirmNew = null
        newGame(l)
    }

    private fun newGame(l: Sudoku.Level) {
        level = l
        sudoku = null
        board.sudoku = null
        board.selected = -1
        status.setText(R.string.sudoku_generating)
        val size = sizeForScreen()
        buildPad(size)
        app.io.execute {
            val generated = Sudoku.generate(size, l)
            runOnUiThread {
                if (isFinishing || level != l || sudoku != null) return@runOnUiThread
                elapsedMs = 0
                resumedAt = SystemClock.elapsedRealtime()
                // The old save goes only once the new grid is there.
                store.clearSudoku()
                show(generated)
            }
        }
    }

    private fun show(s: Sudoku) {
        sudoku = s
        board.sudoku = s
        board.selected = -1
        movesSinceRefresh = 0
        update()
        FullRefresh.flash(flash)
    }

    private fun enter(digit: Int) {
        val s = sudoku ?: return
        confirmNew = null
        val index = board.selected
        if (index < 0 || s.solved) return
        s.set(index, digit)
        if (s.solved) {
            elapsedMs += SystemClock.elapsedRealtime() - resumedAt
            resumedAt = SystemClock.elapsedRealtime()
            store.offerBest(bestKey(s), elapsedMs)
            store.clearSudoku()
            board.selected = -1
        }
        board.invalidate()
        update()
        val every = app.prefs.refreshEvery
        if (every != Prefs.REFRESH_OFF && ++movesSinceRefresh >= every * 2) {
            movesSinceRefresh = 0
            FullRefresh.flash(flash)
        }
    }

    private fun bestKey(s: Sudoku) = "sudoku/${s.size}/${level.name}"

    private fun update() {
        levelButtons.forEach { (l, b) -> b.isSelected = l == level }
        val s = sudoku ?: return
        val bestMs = store.best(bestKey(s))
        best.text = if (bestMs > 0) getString(R.string.games_best, GameStore.time(bestMs)) else ""
        status.text = when {
            s.solved -> getString(R.string.sudoku_solved, GameStore.time(elapsedMs))
            s.filled == s.cells.size -> resources.getQuantityString(R.plurals.sudoku_wrong, s.errors(), s.errors())
            else -> getString(R.string.sudoku_filled, s.filled, s.cells.size)
        }
    }

    override fun onResume() {
        super.onResume()
        resumedAt = SystemClock.elapsedRealtime()
    }

    override fun onPause() {
        super.onPause()
        val s = sudoku ?: return
        if (s.solved) return
        elapsedMs += SystemClock.elapsedRealtime() - resumedAt
        resumedAt = SystemClock.elapsedRealtime()
        store.saveSudoku(s, level, elapsedMs)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = keys.handle(event) || super.dispatchKeyEvent(event)
}
