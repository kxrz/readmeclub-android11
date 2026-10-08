package club.readme.android.reader

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import io.nayuki.qrcodegen.QrCode

/** Renders a QR code as a crisp black-on-white bitmap (vendored Nayuki generator, MIT). */
object QrBitmap {

    private const val BORDER_MODULES = 4

    fun of(text: String, modulePx: Int): Bitmap {
        val qr = QrCode.encodeText(text, QrCode.Ecc.MEDIUM)
        val size = (qr.size + BORDER_MODULES * 2) * modulePx
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val black = Paint().apply { color = Color.BLACK }
        for (y in 0 until qr.size) {
            for (x in 0 until qr.size) {
                if (!qr.getModule(x, y)) continue
                val left = ((x + BORDER_MODULES) * modulePx).toFloat()
                val top = ((y + BORDER_MODULES) * modulePx).toFloat()
                canvas.drawRect(left, top, left + modulePx, top + modulePx, black)
            }
        }
        return bitmap
    }
}
