package club.readme.android.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import club.readme.android.R

/** The Sudoku grid: thick lines around boxes, givens in bold, the selected cell inverted. */
class SudokuView(context: Context, attrs: AttributeSet?) : BoardView(context, attrs) {

    var sudoku: Sudoku? = null
        set(value) {
            field = value
            invalidate()
        }
    var selected = -1
        set(value) {
            field = value
            invalidate()
        }
    var onSelect: (Int) -> Unit = {}

    override val n: Int get() = sudoku?.size ?: 1

    private val thin = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = density }
    private val thick = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 3 * density }
    private val fill = Paint().apply { color = Color.BLACK }
    private val givenText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = context.resources.getFont(R.font.space_mono_bold)
    }
    private val playerText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
    }

    override fun onDraw(canvas: Canvas) {
        val s = sudoku ?: return
        givenText.textSize = cell * 0.55f
        playerText.textSize = cell * 0.55f
        for (i in 0 until n * n) {
            val x = boardLeft + (i % n) * cell
            val y = boardTop + (i / n) * cell
            val inverted = i == selected
            if (inverted) canvas.drawRect(x, y, x + cell, y + cell, fill)
            val digit = s.cells[i]
            if (digit != 0) {
                val paint = if (s.given[i]) givenText else playerText
                paint.color = if (inverted) Color.WHITE else Color.BLACK
                canvas.drawText(digit.toString(), x + cell / 2, y + cell / 2 - (paint.ascent() + paint.descent()) / 2, paint)
            }
        }
        for (k in 0..n) {
            val vertical = if (k % s.boxCols == 0) thick else thin
            val horizontal = if (k % s.boxRows == 0) thick else thin
            canvas.drawLine(boardLeft + k * cell, boardTop, boardLeft + k * cell, boardTop + n * cell, vertical)
            canvas.drawLine(boardLeft, boardTop + k * cell, boardLeft + n * cell, boardTop + k * cell, horizontal)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val i = cellAt(event.x, event.y)
            if (i >= 0) onSelect(i)
        }
        return true
    }
}
