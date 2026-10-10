package club.readme.android.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import club.readme.android.R

/**
 * The Mines grid. Closed cells are white with a border, open ones grey with their count,
 * flags are drawn as small black flags, mines (after a loss) as black discs.
 * Tap to open, long press to flag.
 */
class MinesView(context: Context, attrs: AttributeSet?) : BoardView(context, attrs) {

    var mines: Mines? = null
        set(value) {
            field = value
            invalidate()
        }
    var onTap: (Int) -> Unit = {}
    var onLongPress: (Int) -> Unit = {}

    override val n: Int get() = mines?.size ?: 1

    private val line = Paint().apply { color = context.getColor(R.color.ds_ink); style = Paint.Style.STROKE; strokeWidth = 2 * density }
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.ds_ink) }
    private val openFill = Paint().apply { color = context.getColor(R.color.ds_soft) }
    private val number = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ds_ink)
        textAlign = Paint.Align.CENTER
        typeface = context.resources.getFont(R.font.space_mono_bold)
    }

    private val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            cellAt(e.x, e.y).takeIf { it >= 0 }?.let(onTap)
            return true
        }
        override fun onLongPress(e: MotionEvent) {
            cellAt(e.x, e.y).takeIf { it >= 0 }?.let(onLongPress)
        }
    })

    override fun onDraw(canvas: Canvas) {
        val m = mines ?: return
        number.textSize = cell * 0.5f
        val showMines = m.state == Mines.State.LOST
        for (i in 0 until n * n) {
            val x = boardLeft + (i % n) * cell
            val y = boardTop + (i / n) * cell
            val cx = x + cell / 2
            val cy = y + cell / 2
            if (m.open[i]) canvas.drawRect(x, y, x + cell, y + cell, openFill)
            when {
                showMines && m.isMine(i) -> canvas.drawCircle(cx, cy, cell * 0.25f, ink)
                m.flag[i] -> drawFlag(canvas, cx, cy)
                m.open[i] -> m.count(i).takeIf { it > 0 }?.let {
                    canvas.drawText(it.toString(), cx, cy - (number.ascent() + number.descent()) / 2, number)
                }
            }
        }
        for (k in 0..n) {
            canvas.drawLine(boardLeft + k * cell, boardTop, boardLeft + k * cell, boardTop + n * cell, line)
            canvas.drawLine(boardLeft, boardTop + k * cell, boardLeft + n * cell, boardTop + k * cell, line)
        }
    }

    private fun drawFlag(canvas: Canvas, cx: Float, cy: Float) {
        val h = cell * 0.5f
        canvas.drawRect(cx - h * 0.2f, cy - h / 2, cx - h * 0.1f, cy + h / 2, ink)
        val path = Path().apply {
            moveTo(cx - h * 0.1f, cy - h / 2)
            lineTo(cx + h * 0.4f, cy - h * 0.25f)
            lineTo(cx - h * 0.1f, cy)
            close()
        }
        canvas.drawPath(path, ink)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean = gestures.onTouchEvent(event)
}
