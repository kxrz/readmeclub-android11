package club.readme.android.eink

import android.graphics.Color
import android.view.View

/**
 * Forces a full e-ink refresh by flashing [overlay] (a full-screen, normally GONE view)
 * black then white for ~120 ms. Verified on the S4: clears ghosting.
 */
object FullRefresh {

    fun flash(overlay: View) {
        overlay.setBackgroundColor(Color.BLACK)
        overlay.visibility = View.VISIBLE
        overlay.postDelayed({ overlay.setBackgroundColor(Color.WHITE) }, 60)
        overlay.postDelayed({ overlay.visibility = View.GONE }, 120)
    }
}
