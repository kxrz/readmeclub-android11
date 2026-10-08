package club.readme.android.ui.wallpapers

import android.app.Activity
import android.content.ContentValues
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import club.readme.android.R
import club.readme.android.app
import club.readme.android.eink.FullRefresh
import club.readme.android.eink.PageKeys
import java.io.File

/**
 * Full-screen preview of one wallpaper and a Save button (Pictures/ReadmeClub).
 * There is no "set as wallpaper": the S4 ignores Android's WallpaperManager.
 */
class WallpaperActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var actions: View
    private var file: File? = null

    // Swallow the capacitive button so it never changes the volume here.
    private val keys = PageKeys(onNext = {})

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wallpaper)
        status = findViewById(R.id.status)
        actions = findViewById(R.id.actions)
        val id = intent.getStringExtra(EXTRA_ID) ?: return finish()
        findViewById<TextView>(R.id.title).text = intent.getStringExtra(EXTRA_TITLE)
        val author = intent.getStringExtra(EXTRA_AUTHOR)
        val member = findViewById<TextView>(R.id.member)
        member.text = author

        findViewById<View>(R.id.back).setOnClickListener { finish() }
        findViewById<View>(R.id.save).setOnClickListener { file?.let(::save) }

        status.setText(R.string.loading)
        app.io.execute {
            val full = app.wallpapers.full(id)
            val bitmap = full?.let { BitmapFactory.decodeFile(it.path) }
            val number = app.wallpapers.memberNumber(id)
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                // "#042 · pseudo", like "Shared by" on the site.
                member.text = listOfNotNull(number, author).joinToString(" · ")
                if (full == null || bitmap == null) {
                    status.setText(R.string.wallpaper_unavailable)
                    return@runOnUiThread
                }
                file = full
                findViewById<ImageView>(R.id.preview).setImageBitmap(bitmap)
                status.text = ""
                actions.visibility = View.VISIBLE
                FullRefresh.flash(findViewById(R.id.flash))
            }
        }
    }

    /** Copies the original file into Pictures/ReadmeClub through MediaStore (no permission needed on Android 11). */
    private fun save(file: File) {
        val mime = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            .also { BitmapFactory.decodeFile(file.path, it) }.outMimeType ?: "image/jpeg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "readmeclub-${file.name}.${mime.substringAfter('/')}")
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/ReadmeClub")
        }
        try {
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("MediaStore refused the file")
            contentResolver.openOutputStream(uri)!!.use { out -> file.inputStream().use { it.copyTo(out) } }
            status.setText(R.string.wallpaper_saved)
        } catch (e: Exception) {
            status.text = getString(R.string.wallpaper_failed, e.message)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = keys.handle(event) || super.dispatchKeyEvent(event)

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_AUTHOR = "author"
    }
}
