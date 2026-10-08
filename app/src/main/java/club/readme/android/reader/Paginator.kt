package club.readme.android.reader

object Paginator {

    /**
     * Splits laid-out lines into pages of [pageHeight] px.
     *
     * [lineTops] holds the top of each line followed by the bottom of the last one
     * (so `lineCount + 1` entries, like `Layout.getLineTop(0..lineCount)`).
     * Returns the index of the first line of each page. A line taller than a page
     * (an oversized image) gets a page to itself rather than being cut.
     */
    fun pageStarts(lineTops: IntArray, pageHeight: Int): IntArray {
        val lineCount = lineTops.size - 1
        if (lineCount <= 0) return intArrayOf(0)
        val starts = mutableListOf(0)
        for (line in 1 until lineCount) {
            val pageTop = lineTops[starts.last()]
            if (lineTops[line + 1] - pageTop > pageHeight) starts += line
        }
        return starts.toIntArray()
    }
}
