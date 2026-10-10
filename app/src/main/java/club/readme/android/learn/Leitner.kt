package club.readme.android.learn

/**
 * "Questions I missed", as flashcards in three Leitner boxes. A missed question goes to box 1;
 * "I knew it" moves a card up a box, "Not yet" back to box 1, and knowing it in box 3 retires
 * it. Box 1 comes up every session, box 2 every 2nd, box 3 every 4th.
 */
class Leitner(private val boxes: MutableMap<String, Int> = linkedMapOf(), var session: Int = 0) {

    val size: Int get() = boxes.size

    fun box(id: String): Int = boxes[id] ?: 0

    fun add(id: String) {
        boxes[id] = 1
    }

    fun knew(id: String) {
        val box = boxes[id] ?: return
        if (box >= BOXES) boxes.remove(id) else boxes[id] = box + 1
    }

    fun notYet(id: String) {
        if (id in boxes) boxes[id] = 1
    }

    /** The cards for this session, lowest box first; when none is due, the lowest boxes anyway. */
    fun due(limit: Int = QuizRound.SIZE): List<String> {
        val byBox = boxes.entries.sortedBy { it.value }
        val due = byBox.filter { session % (1 shl (it.value - 1)) == 0 }
        return (due.ifEmpty { byBox }).take(limit).map { it.key }
    }

    /** "id:box,id:box|session", for storage. */
    fun encode(): String = boxes.entries.joinToString(",") { "${it.key}:${it.value}" } + "|" + session

    companion object {
        const val BOXES = 3

        fun decode(s: String?): Leitner {
            if (s.isNullOrEmpty()) return Leitner()
            val (cards, session) = s.split("|").let { it[0] to (it.getOrNull(1)?.toIntOrNull() ?: 0) }
            val boxes = linkedMapOf<String, Int>()
            for (entry in cards.split(",")) {
                val id = entry.substringBeforeLast(":", "")
                val box = entry.substringAfterLast(":").toIntOrNull()
                if (id.isNotEmpty() && box != null && box in 1..BOXES) boxes[id] = box
            }
            return Leitner(boxes, session)
        }
    }
}
