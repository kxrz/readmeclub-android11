package club.readme.android.reader

import android.content.Context
import android.graphics.Canvas
import android.text.Layout
import android.text.Spanned
import android.text.style.URLSpan
import android.util.AttributeSet
import android.view.View

/** Draws one page of a pre-laid-out text: the lines from pageStarts[page] up to the next page's first line. */
class PageView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var layout: Layout? = null
    private var pageStarts = intArrayOf(0)

    var page = 0
        private set
    val pageCount: Int get() = pageStarts.size

    /** Usable text area, inside the padding. */
    val contentWidth: Int get() = width - paddingLeft - paddingRight
    val contentHeight: Int get() = height - paddingTop - paddingBottom

    fun setContent(layout: Layout, pageStarts: IntArray) {
        this.layout = layout
        this.pageStarts = pageStarts
        page = 0
        invalidate()
    }

    /** Character offset where each page starts. */
    val pageOffsets: IntArray
        get() = layout?.let { l -> IntArray(pageCount) { l.getLineStart(pageStarts[it]) } } ?: intArrayOf(0)

    /** End offset of the laid-out text. */
    val textLength: Int get() = layout?.text?.length ?: 0

    fun goTo(target: Int) {
        page = target.coerceIn(0, pageCount - 1)
        invalidate()
    }

    /** Returns false when already at the end (or start) and nothing changed. */
    fun turn(delta: Int): Boolean {
        val target = (page + delta).coerceIn(0, pageCount - 1)
        if (target == page) return false
        page = target
        invalidate()
        return true
    }

    /** The link under a tap at ([x], [y]) in view coordinates on the current page, or null. */
    fun linkAt(x: Float, y: Float): URLSpan? {
        val layout = layout ?: return null
        val text = layout.text as? Spanned ?: return null
        val docY = (y - paddingTop).toInt() + layout.getLineTop(pageStarts[page])
        val line = layout.getLineForVertical(docY)
        val docX = x - paddingLeft
        if (docX < layout.getLineLeft(line) || docX > layout.getLineRight(line)) return null
        val offset = layout.getOffsetForHorizontal(line, docX)
        return text.getSpans(offset, offset, URLSpan::class.java).firstOrNull()
    }

    override fun onDraw(canvas: Canvas) {
        val layout = layout ?: return
        val top = layout.getLineTop(pageStarts[page])
        val endLine = if (page + 1 < pageCount) pageStarts[page + 1] else layout.lineCount
        val bottom = layout.getLineTop(endLine)
        canvas.save()
        canvas.translate(paddingLeft.toFloat(), (paddingTop - top).toFloat())
        canvas.clipRect(0, top, contentWidth, bottom)
        layout.draw(canvas)
        canvas.restore()
    }
}
