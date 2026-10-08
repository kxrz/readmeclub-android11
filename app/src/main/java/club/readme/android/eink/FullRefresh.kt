package club.readme.android.eink

import android.graphics.Color
import android.view.View
import club.readme.android.app
import club.readme.android.data.Prefs

/**
 * Forces a full e-ink refresh by flashing [overlay] (a full-screen, normally GONE view)
 * black then white for ~120 ms. Verified on the S4: clears ghosting.
 * Skipped when the reader turned forced refreshes off (readers with their own refresh modes).
 */
object FullRefresh {

    fun flash(overlay: View, force: Boolean = false) {
        if (!force && overlay.context.app.prefs.refreshEvery == Prefs.REFRESH_OFF) return
        overlay.setBackgroundColor(Color.BLACK)
        overlay.visibility = View.VISIBLE
        overlay.postDelayed({ overlay.setBackgroundColor(Color.WHITE) }, 60)
        overlay.postDelayed({ overlay.visibility = View.GONE }, 120)
    }
}
