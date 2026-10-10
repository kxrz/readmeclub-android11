package club.readme.android.learn

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import club.readme.android.R
import club.readme.android.eink.FullRefresh
import club.readme.android.eink.PageKeys

/** "Questions I missed": up to 10 due cards, each answered "I knew it" or "Not yet" (see [Leitner]). */
class FlashcardsActivity : Activity() {

    private lateinit var store: LearnStore
    private lateinit var deck: Leitner
    private lateinit var cards: List<Question>
    private lateinit var primary: TextView
    private var index = 0
    private var shown = false

    // A page key shows the answer.
    private val keys = PageKeys(onNext = { if (!shown && index < cards.size) showAnswer() })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.flashcards)
        store = LearnStore(this)
        deck = store.deck()
        // Cards whose question is no longer in any pack (a removed or edited pack) leave the deck,
        // so they never take the place of real ones.
        val orphans = deck.ids().filter { store.question(it) == null }
        if (orphans.isNotEmpty()) {
            orphans.forEach(deck::remove)
            store.saveDeck(deck)
        }
        cards = deck.due().mapNotNull(store::question)
        primary = findViewById(R.id.primary)
        findViewById<View>(R.id.back).setOnClickListener { finish() }
        primary.setOnClickListener { if (index < cards.size) showAnswer() else finish() }
        findViewById<View>(R.id.knew).setOnClickListener { grade(knew = true) }
        findViewById<View>(R.id.not_yet).setOnClickListener { grade(knew = false) }
        showCard()
    }

    private fun showCard() {
        shown = false
        findViewById<View>(R.id.back_side).visibility = View.GONE
        findViewById<View>(R.id.grade).visibility = View.GONE
        primary.visibility = View.VISIBLE
        val status = findViewById<TextView>(R.id.status)
        val box = findViewById<TextView>(R.id.box)
        val question = findViewById<TextView>(R.id.question)
        findViewById<View>(R.id.hint).visibility = if (index < cards.size) View.VISIBLE else View.GONE
        // A card's question large; the messages (empty deck, done) at reading size.
        question.textSize = if (index < cards.size) 24f else 18f
        if (index >= cards.size) {
            findViewById<TextView>(R.id.theme).setText(R.string.learn_missed)
            box.visibility = View.GONE
            findViewById<TextView>(R.id.question).text = when {
                cards.isEmpty() -> getString(R.string.cards_empty)
                else -> resources.getQuantityString(R.plurals.cards_done, deck.size, deck.size)
            }
            status.text = ""
            primary.setText(R.string.cards_finish)
        } else {
            val card = cards[index]
            findViewById<TextView>(R.id.theme).text = card.theme
            box.visibility = View.VISIBLE
            box.text = getString(R.string.cards_box, deck.box(card.id), Leitner.BOXES)
            findViewById<TextView>(R.id.question).text = card.text
            status.text = getString(R.string.page_of, index + 1, cards.size)
            primary.setText(R.string.cards_show)
        }
        FullRefresh.flash(findViewById(R.id.flash))
    }

    private fun showAnswer() {
        shown = true
        val card = cards[index]
        findViewById<TextView>(R.id.answer).text = card.answers[0]
        findViewById<TextView>(R.id.why).text = card.why
        findViewById<View>(R.id.hint).visibility = View.GONE
        // When the card comes back: a box up shows half as often; known in the last box, it leaves.
        val box = deck.box(card.id)
        findViewById<TextView>(R.id.knew).text =
            if (box >= Leitner.BOXES) getString(R.string.cards_knew_done) else getString(R.string.cards_knew_back, 1 shl box)
        findViewById<TextView>(R.id.not_yet).setText(R.string.cards_not_yet_back)
        findViewById<View>(R.id.back_side).visibility = View.VISIBLE
        findViewById<View>(R.id.grade).visibility = View.VISIBLE
        primary.visibility = View.GONE
        FullRefresh.flash(findViewById(R.id.flash))
    }

    private fun grade(knew: Boolean) {
        // A session counts once a card is graded, not when the screen merely opens.
        if (index == 0) deck.session++
        val id = cards[index].id
        if (knew) deck.knew(id) else deck.notYet(id)
        store.saveDeck(deck)
        index++
        showCard()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = keys.handle(event) || super.dispatchKeyEvent(event)
}
