package club.readme.android.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingMathTest {

    private val starts = intArrayOf(0, 100, 250, 400)

    @Test fun offsetOnAPageStart() = assertEquals(2, ReadingMath.pageForOffset(starts, 250))
    @Test fun offsetInsideAPage() = assertEquals(1, ReadingMath.pageForOffset(starts, 180))
    @Test fun offsetPastTheEnd() = assertEquals(3, ReadingMath.pageForOffset(starts, 9999))
    @Test fun noSavedOffset() = assertEquals(0, ReadingMath.pageForOffset(starts, -1))

    @Test fun countsWords() = assertEquals(6, ReadingMath.countWords("Flash it — then, back up first!", 0, 31))
    @Test fun countsWordsInARange() = assertEquals(2, ReadingMath.countWords("one two three four", 4, 13))

    @Test fun minutesLeftFromCurrentPage() =
        // 230 + 230 + 115 words from page 1 on: 575 words, 2.5 min, rounded up.
        assertEquals(3, ReadingMath.minutesLeft(intArrayOf(500, 230, 230, 115), 1))

    @Test fun neverLessThanAMinute() = assertEquals(1, ReadingMath.minutesLeft(intArrayOf(10), 0))

    @Test fun headingsInOrderWithDuplicates() {
        val text = "Intro\nBack up\ntext\nFlash\nmore\nBack up\nend"
        assertEquals(listOf(6, 19, 30, -1), ReadingMath.headingOffsets(text, listOf("Back up", "Flash", "Back up", "Missing")))
    }
}
