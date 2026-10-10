package club.readme.android.game

import kotlin.random.Random

/**
 * Minesweeper on a [size] × [size] grid with [mineCount] mines. Mines are placed on the first
 * opening, away from that cell and its neighbours, so the first tap is always safe.
 */
class Mines(val size: Int, val mineCount: Int) {

    enum class State { PLAYING, WON, LOST }

    private val mine = BooleanArray(size * size)
    val open = BooleanArray(size * size)
    val flag = BooleanArray(size * size)
    var state = State.PLAYING
        private set
    var started = false
        private set

    val flags: Int get() = flag.count { it }

    fun isMine(index: Int) = mine[index]

    /** Mines around [index] (0–8). */
    fun count(index: Int): Int = neighbours(index).count { mine[it] }

    fun toggleFlag(index: Int) {
        if (state == State.PLAYING && !open[index]) flag[index] = !flag[index]
    }

    /** Opens [index]; a zero opens its whole empty area. */
    fun open(index: Int, random: Random = Random.Default) {
        if (state != State.PLAYING || open[index] || flag[index]) return
        if (!started) {
            place(index, random)
            started = true
        }
        if (mine[index]) {
            open[index] = true
            state = State.LOST
            return
        }
        val stack = ArrayDeque<Int>().apply { add(index) }
        while (stack.isNotEmpty()) {
            val i = stack.removeLast()
            if (open[i] || flag[i]) continue
            open[i] = true
            if (count(i) == 0) neighbours(i).filterTo(stack) { !open[it] && !mine[it] }
        }
        if (open.indices.all { open[it] || mine[it] }) state = State.WON
    }

    private fun place(safe: Int, random: Random) {
        val keepClear = neighbours(safe) + safe
        (0 until size * size).filter { it !in keepClear }.shuffled(random).take(mineCount).forEach { mine[it] = true }
    }

    fun neighbours(index: Int): List<Int> {
        val row = index / size
        val col = index % size
        val result = ArrayList<Int>(8)
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val r = row + dr
            val c = col + dc
            if (r in 0 until size && c in 0 until size) result += r * size + c
        }
        return result
    }

    companion object {
        /** About one cell in six is a mine: 8 on 7 × 7, 16 on 10 × 10. */
        fun forSize(size: Int) = Mines(size, if (size <= 7) 8 else 16)
    }
}
