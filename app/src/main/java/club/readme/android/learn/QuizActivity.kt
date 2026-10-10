package club.readme.android.learn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import club.readme.android.R
import club.readme.android.eink.FullRefresh
import club.readme.android.eink.PageKeys

/**
 * A round of 10 questions from a pack. Each answer is followed by the right answer and why;
 * missed questions go to the "Questions I missed" flashcards. One full refresh per screen.
 */
class QuizActivity : Activity() {

    private lateinit var store: LearnStore
    private lateinit var pack: QuizPack
    private lateinit var round: QuizRound
    private lateinit var answers: List<TextView>
    private lateinit var primary: TextView
    private lateinit var status: TextView
    private var done = false

    // A page key moves on once the answer is shown.
    private val keys = PageKeys(onNext = { if (round.chosen >= 0) advance() })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = LearnStore(this)
        pack = store.pack(intent.getStringExtra(EXTRA_PACK).orEmpty()) ?: return finish()
        setContentView(R.layout.quiz)
        answers = listOf(R.id.a0, R.id.a1, R.id.a2, R.id.a3).map { findViewById(it) }
        primary = findViewById(R.id.primary)
        status = findViewById(R.id.status)
        findViewById<View>(R.id.back).setOnClickListener { finish() }
        answers.forEachIndexed { i, view -> view.setOnClickListener { choose(i) } }
        primary.setOnClickListener { if (done) newRound() else advance() }
        newRound()
    }

    private fun newRound() {
        round = QuizRound(QuizRound.draw(pack, store.seen(pack.id)))
        store.markSeen(pack, round.items.map { it.question.id })
        done = false
        showQuestion()
    }

    private fun showQuestion() {
        val item = round.current
        findViewById<TextView>(R.id.theme).text = item.question.theme
        findViewById<TextView>(R.id.progress).text = getString(R.string.page_of, round.index + 1, round.items.size)
        findViewById<TextView>(R.id.question).text = item.question.text
        item.answers.forEachIndexed { i, text -> answers[i].text = text }
        findViewById<View>(R.id.answers).visibility = View.VISIBLE
        findViewById<View>(R.id.feedback).visibility = View.GONE
        primary.visibility = View.GONE
        status.text = resources.getQuantityString(R.plurals.quiz_score, round.score, round.score)
        FullRefresh.flash(findViewById(R.id.flash))
    }

    private fun choose(choice: Int) {
        if (round.chosen >= 0) return
        val right = round.answer(choice)
        if (!right) {
            val deck = store.deck()
            deck.add(round.current.question.id)
            store.saveDeck(deck)
        }
        findViewById<TextView>(R.id.verdict).setText(if (right) R.string.quiz_right else R.string.quiz_wrong)
        findViewById<TextView>(R.id.right_answer).text = round.current.question.answers[0]
        findViewById<TextView>(R.id.why).text = round.current.question.why
        findViewById<View>(R.id.answers).visibility = View.GONE
        findViewById<View>(R.id.feedback).visibility = View.VISIBLE
        primary.setText(if (round.isLast) R.string.quiz_score_button else R.string.next)
        primary.visibility = View.VISIBLE
        status.text = resources.getQuantityString(R.plurals.quiz_score, round.score, round.score)
        FullRefresh.flash(findViewById(R.id.flash))
    }

    private fun advance() {
        if (done) return
        if (round.next()) showQuestion() else showScore()
    }

    private fun showScore() {
        done = true
        val best = store.best(pack.id)
        val record = store.offerBest(pack.id, round.score)
        findViewById<TextView>(R.id.theme).text = pack.title
        findViewById<TextView>(R.id.progress).text = getString(R.string.quiz_done)
        findViewById<TextView>(R.id.question).text = getString(R.string.quiz_final, round.score, round.items.size)
        findViewById<TextView>(R.id.verdict).setText(if (record && best >= 0) R.string.quiz_record else R.string.quiz_round_over)
        findViewById<TextView>(R.id.right_answer).text =
            getString(R.string.quiz_best, maxOf(best, round.score), round.items.size)
        findViewById<TextView>(R.id.why).text = if (round.missed.isEmpty()) getString(R.string.quiz_none_missed)
        else resources.getQuantityString(R.plurals.quiz_missed, round.missed.size, round.missed.size)
        findViewById<View>(R.id.answers).visibility = View.GONE
        findViewById<View>(R.id.feedback).visibility = View.VISIBLE
        primary.setText(R.string.quiz_again)
        status.text = ""
        FullRefresh.flash(findViewById(R.id.flash))
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = keys.handle(event) || super.dispatchKeyEvent(event)

    companion object {
        private const val EXTRA_PACK = "pack"

        fun intent(context: Context, pack: String): Intent = Intent(context, QuizActivity::class.java).putExtra(EXTRA_PACK, pack)
    }
}
