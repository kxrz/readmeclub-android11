package club.readme.android

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import club.readme.android.eink.FullRefresh
import club.readme.android.eink.PageKeys

/** Every design-system component on one screen (from the diagnostic), to check them on a reader. */
class ComponentsActivity : Activity() {

    // Swallow the page keys: nothing to turn here.
    private val keys = PageKeys(onNext = {})

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ds_components)
        val flash = findViewById<View>(R.id.flash)
        findViewById<View>(R.id.back).setOnClickListener { finish() }
        findViewById<View>(R.id.refresh).setOnClickListener { FullRefresh.flash(flash, force = true) }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = keys.handle(event) || super.dispatchKeyEvent(event)
}
