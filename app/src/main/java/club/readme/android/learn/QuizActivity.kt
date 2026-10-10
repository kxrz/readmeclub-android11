package club.readme.android.learn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
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
    private var startedAt = 0L

    // A page key moves on once the answer is shown.
    private val keys = PageKeys(onNext = { if (round.chosen != NONE) advance() })

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
        // Skip before answering (it counts as missed), Next after.
        primary.setOnClickListener {
            when {
                done -> newRound()
                round.chosen == NONE -> choose(SKIP)
                else -> advance()
            }
        }
        findViewById<View>(R.id.review).setOnClickListener {
            startActivity(Intent(this, FlashcardsActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.other_packs).setOnClickListener {
            startActivity(Intent(this, PacksActivity::class.java))
            finish()
        }
        findViewById<TextView>(R.id.pack).text = pack.title
        newRound()
    }

    private fun newRound() {
        round = QuizRound(QuizRound.draw(pack, store.seen(pack.id)))
        store.markSeen(pack, round.items.map { it.question.id })
        done = false
        startedAt = SystemClock.elapsedRealtime()
        showQuestion()
    }

    private fun showQuestion() {
        val item = round.current
        findViewById<TextView>(R.id.theme).text = item.question.theme
        findViewById<TextView>(R.id.progress).text = getString(R.string.page_of, round.index + 1, round.items.size)
        findViewById<TextView>(R.id.question).text = item.question.text
        item.answers.forEachIndexed { i, text ->
            answers[i].text = "${LETTERS[i]}   $text"
            answers[i].isSelected = false
            answers[i].isEnabled = true
        }
        findViewById<View>(R.id.play).visibility = View.VISIBLE
        findViewById<View>(R.id.result).visibility = View.GONE
        findViewById<View>(R.id.feedback).visibility = View.GONE
        findViewById<View>(R.id.added).visibility = View.GONE
        primary.setText(R.string.quiz_skip)
        primary.visibility = View.VISIBLE
        status.text = resources.getQuantityString(R.plurals.quiz_score, round.score, round.score)
        FullRefresh.flash(findViewById(R.id.flash))
    }

    /** Answers [choice] (or [SKIP]); the four answers stay, marked: the right one, and yours. */
    private fun choose(choice: Int) {
        if (round.chosen != NONE) return
        val right = round.answer(choice)
        if (!right) {
            val deck = store.deck()
            deck.add(round.current.question.id)
            store.saveDeck(deck)
        }
        val item = round.current
        item.answers.forEachIndexed { i, text ->
            val view = answers[i]
            view.isEnabled = false
            when (i) {
                item.right -> {
                    view.text = getString(R.string.quiz_mark_right, text)
                    view.isSelected = true
                }
                choice -> view.text = SpannableString(getString(R.string.quiz_mark_yours, text)).apply {
                    setSpan(StrikethroughSpan(), 4, 4 + text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
        findViewById<TextView>(R.id.why).text = item.question.why
        findViewById<View>(R.id.feedback).visibility = View.VISIBLE
        findViewById<View>(R.id.added).visibility = if (right) View.GONE else View.VISIBLE
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
        val total = round.items.size
        val minutes = maxOf(1, ((SystemClock.elapsedRealtime() - startedAt) / 60_000).toInt())
        findViewById<TextView>(R.id.progress).text = getString(R.string.quiz_done)
        findViewById<TextView>(R.id.verdict).setText(if (record && best >= 0) R.string.quiz_record else R.string.quiz_round_over)
        findViewById<TextView>(R.id.score).text = getString(R.string.quiz_final, round.score, total)
        findViewById<TextView>(R.id.comment).setText(
            when {
                round.score == total -> R.string.quiz_comment_all
                round.score * 10 >= total * 8 -> R.string.quiz_comment_good
                round.score * 10 >= total * 5 -> R.string.quiz_comment_fair
                else -> R.string.quiz_comment_low
            },
        )
        findViewById<TextView>(R.id.score_meta).text =
            if (best >= 0) getString(R.string.quiz_previous_best, best, total, minutes) else getString(R.string.quiz_minutes, minutes)
        // The questions missed, three at most: the rest wait in the flashcards.
        val missed = round.missed
        findViewById<TextView>(R.id.missed_label).text =
            if (missed.isEmpty()) getString(R.string.quiz_none_missed) else getString(R.string.quiz_missed_label)
        findViewById<TextView>(R.id.missed).text = missed.take(MISSED_SHOWN).joinToString("\n") { "• ${it.text}  ${it.answers[0]}" } +
            if (missed.size > MISSED_SHOWN) "\n" + getString(R.string.quiz_missed_more, missed.size - MISSED_SHOWN) else ""
        findViewById<View>(R.id.review).visibility = if (missed.isEmpty()) View.GONE else View.VISIBLE
        findViewById<View>(R.id.play).visibility = View.GONE
        findViewById<View>(R.id.result).visibility = View.VISIBLE
        primary.setText(R.string.quiz_again)
        status.text = ""
        FullRefresh.flash(findViewById(R.id.flash))
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = keys.handle(event) || super.dispatchKeyEvent(event)

    companion object {
        private const val EXTRA_PACK = "pack"
        private const val NONE = -1
        private const val SKIP = -2
        private const val MISSED_SHOWN = 3
        private val LETTERS = listOf("A", "B", "C", "D")

        fun intent(context: Context, pack: String): Intent = Intent(context, QuizActivity::class.java).putExtra(EXTRA_PACK, pack)
    }
}
