package club.readme.android.eink

import android.view.KeyEvent

/**
 * Page-turn keys. The S4's capacitive button reports KEYCODE_VOLUME_DOWN; many other
 * e-readers send PAGE_DOWN / PAGE_UP from their page buttons. A long press on a "next"
 * key shows up as repeated DOWN events (isLongPress stays false on the S4), so it is
 * detected by hold time. Volume keys are always consumed so they never change the volume.
 */
class PageKeys(
    private val onNext: () -> Unit,
    private val onPrevious: () -> Unit = {},
    private val onLongPress: () -> Unit = {},
) {
    private var longFired = false

    /** Returns true if the event was handled (and must not reach the system). */
    fun handle(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_PAGE_DOWN -> when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (event.repeatCount == 0) {
                        longFired = false
                    } else if (!longFired && event.eventTime - event.downTime >= LONG_PRESS_MS) {
                        longFired = true
                        onLongPress()
                    }
                }
                KeyEvent.ACTION_UP -> if (!longFired) onNext()
            }
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_PAGE_UP -> if (event.action == KeyEvent.ACTION_UP) onPrevious()
            else -> return false
        }
        return true
    }

    private companion object {
        const val LONG_PRESS_MS = 500
    }
}
