package club.readme.android.reader

import kotlin.math.ceil

/** The arithmetic behind resuming, the contents and "min left", kept free of Android for tests. */
object ReadingMath {

    /** Average silent reading speed, words per minute. */
    const val WORDS_PER_MINUTE = 230

    /** Page showing text [offset], given each page's first character offset (ascending). */
    fun pageForOffset(pageStarts: IntArray, offset: Int): Int {
        if (offset <= 0) return 0
        val i = pageStarts.indexOfLast { it <= offset }
        return maxOf(0, i)
    }

    fun countWords(text: CharSequence, start: Int, end: Int): Int {
        var words = 0
        var inWord = false
        for (i in start until end) {
            val letter = text[i].isLetterOrDigit()
            if (letter && !inWord) words++
            inWord = letter
        }
        return words
    }

    /** Minutes to read the current page and the ones after it, at least 1. */
    fun minutesLeft(wordsPerPage: IntArray, page: Int): Int {
        val words = (page until wordsPerPage.size).sumOf { wordsPerPage[it] }
        return maxOf(1, ceil(words.toDouble() / WORDS_PER_MINUTE).toInt())
    }

    /**
     * Character offset of each heading in [text], searched in document order (two sections
     * can share a title); -1 when a heading isn't found.
     */
    fun headingOffsets(text: String, headings: List<String>): List<Int> {
        var from = 0
        return headings.map { heading ->
            val at = text.indexOf(heading.trim(), from)
            if (at >= 0) from = at + 1
            at
        }
    }
}
