package club.readme.android.game

import android.content.Context
import android.util.AttributeSet
import android.view.View

/** A square board centred in the view: subclasses draw [n] × [n] cells of [cell] px. */
abstract class BoardView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    abstract val n: Int

    protected val density = resources.displayMetrics.density

    protected val cell: Float
        get() = minOf(width - paddingLeft - paddingRight, height - paddingTop - paddingBottom).toFloat() / n

    protected val boardLeft: Float get() = (width - cell * n) / 2
    protected val boardTop: Float get() = (height - cell * n) / 2

    /** Index of the cell under (x, y), or -1 outside the board. */
    protected fun cellAt(x: Float, y: Float): Int {
        if (x < boardLeft || y < boardTop) return -1
        val col = ((x - boardLeft) / cell).toInt()
        val row = ((y - boardTop) / cell).toInt()
        return if (row < n && col < n) row * n + col else -1
    }
}
