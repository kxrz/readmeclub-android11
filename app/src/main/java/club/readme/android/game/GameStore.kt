package club.readme.android.game

import android.content.Context

/** Saved games and best scores, on the device only. */
class GameStore(context: Context) {

    private val prefs = context.getSharedPreferences("games", Context.MODE_PRIVATE)

    class SavedSudoku(val sudoku: Sudoku, val level: Sudoku.Level, val elapsedMs: Long)

    fun saveSudoku(s: Sudoku, level: Sudoku.Level, elapsedMs: Long) {
        val given = s.given.joinToString("") { if (it) "1" else "0" }
        prefs.edit().putString(
            "sudoku",
            listOf(s.size, level.name, s.solution.joinToString(""), given, s.cells.joinToString(""), elapsedMs).joinToString("|"),
        ).apply()
    }

    /** The Sudoku in progress, or null (none, or a save this version cannot read). */
    fun loadSudoku(): SavedSudoku? = runCatching {
        val p = prefs.getString("sudoku", null)!!.split("|")
        val size = p[0].toInt()
        fun digits(s: String) = IntArray(s.length) { s[it] - '0' }
        val sudoku = Sudoku(size, digits(p[2]), BooleanArray(p[3].length) { p[3][it] == '1' }, digits(p[4]))
        SavedSudoku(sudoku, Sudoku.Level.valueOf(p[1]), p[5].toLong())
    }.getOrNull()

    fun clearSudoku() = prefs.edit().remove("sudoku").apply()

    val hasSudoku: Boolean get() = prefs.contains("sudoku")

    /** Best time in ms for [key], 0 if none yet. */
    fun best(key: String): Long = prefs.getLong("best/$key", 0)

    /** Records [ms] if it beats the best; true when it does. */
    fun offerBest(key: String, ms: Long): Boolean {
        val best = best(key)
        if (best != 0L && ms >= best) return false
        prefs.edit().putLong("best/$key", ms).apply()
        return true
    }

    companion object {
        fun time(ms: Long): String {
            val s = ms / 1000
            return "%d:%02d".format(s / 60, s % 60)
        }
    }
}
