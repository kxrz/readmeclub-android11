package club.readme.android.reader

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class PaginatorTest {

    /** Tops of [count] lines of [height] px, plus the bottom of the last one. */
    private fun lines(count: Int, height: Int) = IntArray(count + 1) { it * height }

    @Test fun shortTextFitsOnePage() =
        assertArrayEquals(intArrayOf(0), Paginator.pageStarts(lines(5, 20), 400))

    @Test fun emptyTextHasOnePage() =
        assertArrayEquals(intArrayOf(0), Paginator.pageStarts(intArrayOf(0), 400))

    @Test fun paragraphOverThreePages() =
        // 25 lines of 20 px, 10 lines per 200 px page.
        assertArrayEquals(intArrayOf(0, 10, 20), Paginator.pageStarts(lines(25, 20), 200))

    @Test fun lineEndingExactlyAtPageBottomStaysOnThatPage() =
        assertArrayEquals(intArrayOf(0, 10), Paginator.pageStarts(lines(11, 20), 200))

    @Test fun imageTallerThanThePageGetsItsOwnPage() {
        // Lines 0-1 text (20 px), line 2 an image of 500 px, lines 3-4 text.
        val tops = intArrayOf(0, 20, 40, 540, 560, 580)
        assertArrayEquals(intArrayOf(0, 2, 3), Paginator.pageStarts(tops, 200))
    }

    @Test fun oversizedFirstLine() {
        val tops = intArrayOf(0, 500, 520)
        assertArrayEquals(intArrayOf(0, 1), Paginator.pageStarts(tops, 200))
    }
}
