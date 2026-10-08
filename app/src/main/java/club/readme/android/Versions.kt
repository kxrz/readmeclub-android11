package club.readme.android

object Versions {

    /** Compares dotted versions numerically ("0.10.0" > "0.9.1"); missing parts count as 0. */
    fun compare(a: String, b: String): Int {
        val left = a.split('.').map { it.toIntOrNull() ?: 0 }
        val right = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(left.size, right.size)) {
            val diff = left.getOrElse(i) { 0 } - right.getOrElse(i) { 0 }
            if (diff != 0) return diff.coerceIn(-1, 1)
        }
        return 0
    }
}
