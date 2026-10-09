package club.readme.android.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/** The Lights Out grid: black squares are lit, white ones are off. Redrawn once per press, no animation. */
class LightsView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    var game: LightsOut? = null
        set(value) {
            field = value
            invalidate()
        }

    /** Called with (row, col) when a square is tapped. */
    var onPress: (Int, Int) -> Unit = { _, _ -> }

    private val fill = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
    private val line = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2 * resources.displayMetrics.density
    }

    private val cell: Float
        get() = minOf(width - paddingLeft - paddingRight, height - paddingTop - paddingBottom).toFloat() / (game?.size ?: 1)

    private val gridLeft: Float
        get() = (width - cell * (game?.size ?: 0)) / 2

    private val gridTop: Float
        get() = (height - cell * (game?.size ?: 0)) / 2

    override fun onDraw(canvas: Canvas) {
        val game = game ?: return
        val gap = cell * 0.08f
        for (row in 0 until game.size) for (col in 0 until game.size) {
            val x = gridLeft + col * cell
            val y = gridTop + row * cell
            canvas.drawRect(x + gap, y + gap, x + cell - gap, y + cell - gap, if (game.isLit(row, col)) fill else line)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val game = game ?: return false
        if (event.action == MotionEvent.ACTION_UP) {
            val col = ((event.x - gridLeft) / cell).toInt()
            val row = ((event.y - gridTop) / cell).toInt()
            if (event.x >= gridLeft && event.y >= gridTop && row < game.size && col < game.size) onPress(row, col)
        }
        return true
    }
}
