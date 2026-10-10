package club.readme.android.game.stacks

/**
 * SplitMix64: a small seeded generator whose whole state is one Long, so a saved run
 * resumes exactly where it stopped and tests are deterministic.
 */
class Rng(var state: Long) {

    private fun next(): Long {
        state += -7046029254386353131L // 0x9E3779B97F4A7C15
        var z = state
        z = (z xor (z ushr 30)) * -4658895280553007687L // 0xBF58476D1CE4E5B9
        z = (z xor (z ushr 27)) * -7723592293110705685L // 0x94D049BB133111EB
        return z xor (z ushr 31)
    }

    /** 0 until [n]. */
    fun int(n: Int): Int = ((next() ushr 1) % n).toInt()

    fun d6(): Int = int(6) + 1

    fun chance(percent: Int): Boolean = int(100) < percent

    /** One entry of [list], drawn by [weight]; null when the list is empty or weighs nothing. */
    fun <T> pick(list: List<T>, weight: (T) -> Int = { 1 }): T? {
        val total = list.sumOf { maxOf(0, weight(it)) }
        if (total <= 0) return null
        var r = int(total)
        for (x in list) {
            r -= maxOf(0, weight(x))
            if (r < 0) return x
        }
        return null
    }
}
