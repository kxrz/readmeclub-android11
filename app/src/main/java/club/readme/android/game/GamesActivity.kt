package club.readme.android.game

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import club.readme.android.R
import club.readme.android.app
import club.readme.android.eink.FullRefresh
import club.readme.android.eink.PageKeys
import club.readme.android.game.stacks.StacksActivity
import club.readme.android.learn.FlashcardsActivity
import club.readme.android.learn.LearnStore
import club.readme.android.learn.PacksActivity
import club.readme.android.learn.QuizRound
import club.readme.android.learn.QuizActivity

/** Games: Play (Sudoku, Mines, Lights out) and Learn (quiz packs, Questions I missed), all offline. */
class GamesActivity : Activity() {

    private lateinit var store: GameStore
    private lateinit var tabPlay: View
    private lateinit var tabLearn: View
    private lateinit var resume: View

    // Swallow the page keys: there are no pages here.
    private val keys = PageKeys(onNext = {})

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.games)
        store = GameStore(this)
        tabPlay = findViewById(R.id.tab_play)
        tabLearn = findViewById(R.id.tab_learn)
        resume = findViewById(R.id.resume)
        tabPlay.setOnClickListener { showTab(learn = false) }
        tabLearn.setOnClickListener { showTab(learn = true) }
        findViewById<View>(R.id.back).setOnClickListener { finish() }
        resume.setOnClickListener { open(SudokuActivity::class.java) }
        findViewById<View>(R.id.game_stacks).setOnClickListener { open(StacksActivity::class.java) }
        findViewById<View>(R.id.game_sudoku).setOnClickListener { open(SudokuActivity::class.java) }
        findViewById<View>(R.id.game_mines).setOnClickListener { open(MinesActivity::class.java) }
        findViewById<View>(R.id.game_lights).setOnClickListener { open(LightsOutActivity::class.java) }
        showTab(savedInstanceState?.getBoolean(KEY_LEARN) == true)
    }

    private fun open(activity: Class<out Activity>) = startActivity(Intent(this, activity))

    /** Resume Sudoku is Play's main action; Learn has none of its own. */
    private fun updateResume() {
        resume.visibility = if (tabPlay.isSelected && store.hasSudoku) View.VISIBLE else View.GONE
    }

    private fun showTab(learn: Boolean) {
        tabPlay.isSelected = !learn
        tabLearn.isSelected = learn
        findViewById<View>(R.id.play).visibility = if (learn) View.GONE else View.VISIBLE
        findViewById<View>(R.id.learn).visibility = if (learn) View.VISIBLE else View.GONE
        updateResume()
        FullRefresh.flash(findViewById(R.id.flash))
    }

    override fun onResume() {
        super.onResume()
        val sudokuSize = GameStore.sudokuSize(resources.configuration.screenWidthDp)
        val minesSize = GameStore.minesSize(resources.configuration.screenWidthDp)
        tile(R.id.game_stacks, R.string.stacks_title, StacksActivity.summary(this)?.let { (floor, level) ->
            getString(R.string.games_stacks_progress, floor, level)
        } ?: getString(R.string.games_stacks_new))
        tile(R.id.game_sudoku, R.string.sudoku_title, when (val saved = store.loadSudoku()) {
            null -> getString(R.string.games_sudoku_meta, sudokuSize, sudokuSize)
            else -> getString(R.string.games_sudoku_progress, saved.sudoku.filled, saved.sudoku.cells.size)
        })
        tile(R.id.game_mines, R.string.mines_title, getString(R.string.games_mines_meta, minesSize, minesSize, Mines.forSize(minesSize).mineCount) +
            best("mines/$minesSize"))
        val lightsBest = app.prefs.lightsOutBest
        tile(R.id.game_lights, R.string.lights_title, getString(R.string.games_lights_meta) +
            if (lightsBest > 0) " · " + getString(R.string.games_best, resources.getQuantityString(R.plurals.lights_moves, lightsBest, lightsBest)) else "")
        updateResume()
        showLearn()
    }

    /** General knowledge (in the APK), all quiz packs, then Questions I missed. */
    private fun showLearn() {
        val learn = LearnStore(this)
        val general = learn.bundled.first()
        val best = learn.best(general.id)
        tile(R.id.learn_general, 0, getString(R.string.learn_pack_meta, general.questions.size) +
            if (best >= 0) " · " + getString(R.string.quiz_best, best, minOf(QuizRound.SIZE, general.questions.size)) else "")
        findViewById<View>(R.id.learn_general).findViewById<TextView>(R.id.name).text = general.title
        findViewById<View>(R.id.learn_general).setOnClickListener { startActivity(QuizActivity.intent(this, general.id)) }
        val installed = learn.downloadedCount()
        tile(R.id.learn_all, R.string.packs_title, if (installed == 0) getString(R.string.learn_all_meta_none)
            else resources.getQuantityString(R.plurals.learn_all_meta, installed, installed))
        findViewById<View>(R.id.learn_all).setOnClickListener { open(PacksActivity::class.java) }
        val deck = learn.deck()
        tile(R.id.learn_missed, R.string.learn_missed, if (deck.size == 0) getString(R.string.learn_missed_empty)
            else resources.getQuantityString(R.plurals.learn_missed_meta, deck.size, deck.size))
        findViewById<View>(R.id.learn_missed).setOnClickListener { open(FlashcardsActivity::class.java) }
    }

    private fun best(key: String): String {
        val ms = store.best(key)
        return if (ms > 0) " · " + getString(R.string.games_best, GameStore.time(ms)) else ""
    }

    private fun tile(id: Int, name: Int, meta: String) {
        val tile = findViewById<View>(id)
        if (name != 0) tile.findViewById<TextView>(R.id.name).setText(name)
        tile.findViewById<TextView>(R.id.meta).text = meta
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_LEARN, tabLearn.isSelected)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = keys.handle(event) || super.dispatchKeyEvent(event)

    private companion object {
        const val KEY_LEARN = "learn"
    }
}
