package club.readme.android.learn

import kotlin.random.Random

/** A round of questions, each with its answers shuffled. */
class QuizRound(questions: List<Question>, random: Random = Random.Default) {

    class Item(val question: Question, val answers: List<String>, val right: Int)

    val items: List<Item> = questions.map { q ->
        val answers = q.answers.shuffled(random)
        Item(q, answers, answers.indexOf(q.answers[0]))
    }

    var index = 0
        private set
    var score = 0
        private set
    /** The choice made on the current question, or -1 before answering. */
    var chosen = -1
        private set
    val missed = mutableListOf<Question>()

    val current: Item get() = items[index]
    val isLast: Boolean get() = index == items.lastIndex

    /** Records [choice] for the current question (once); true when it is right. */
    fun answer(choice: Int): Boolean {
        if (chosen < 0) {
            chosen = choice
            if (choice == current.right) score++ else missed += current.question
        }
        return chosen == current.right
    }

    /** Moves to the next question; false at the end of the round. */
    fun next(): Boolean {
        if (isLast) return false
        index++
        chosen = -1
        return true
    }

    companion object {
        const val SIZE = 10

        /** [size] questions, not seen yet first ([seen]: ids), in random order. */
        fun draw(pack: QuizPack, seen: Set<String>, size: Int = SIZE, random: Random = Random.Default): List<Question> {
            val (fresh, old) = pack.questions.partition { it.id !in seen }
            return (fresh.shuffled(random) + old.shuffled(random)).take(size)
        }
    }
}
