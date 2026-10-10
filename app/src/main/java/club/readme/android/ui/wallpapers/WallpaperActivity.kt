package club.readme.android.ui.wallpapers

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
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
 * Full-screen preview of one wallpaper, a Save button (Pictures/ReadmeClub, or the folder
 * picked with Folder) and a Folder button. There is no "set as wallpaper": the S4 ignores
 * Android's WallpaperManager.
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
        val card = intent.hasExtra(EXTRA_CARD_DARK)
        val id = intent.getStringExtra(EXTRA_ID)
        if (id == null && !card) return finish()
        findViewById<TextView>(R.id.title).text = intent.getStringExtra(EXTRA_TITLE)
        val author = intent.getStringExtra(EXTRA_AUTHOR)
        val member = findViewById<TextView>(R.id.member)
        member.text = author
        if (card) {
            // The member's own card: one button switches between the light and dark versions.
            val dark = intent.getBooleanExtra(EXTRA_CARD_DARK, false)
            member.setBackgroundResource(R.drawable.ds_button)
            member.minimumHeight = resources.getDimensionPixelSize(R.dimen.ds_touch)
            member.gravity = android.view.Gravity.CENTER
            member.setText(if (dark) R.string.card_light else R.string.card_dark)
            member.setOnClickListener {
                startActivity(cardIntent(this, !dark))
                finish()
            }
        }

        findViewById<View>(R.id.back).setOnClickListener { finish() }
        findViewById<View>(R.id.save).setOnClickListener {
            // Supernote's file browser doesn't show Pictures: ask for a folder it shows first.
            if (app.prefs.wallpaperFolder == null && Build.MANUFACTURER.equals("Supernote", ignoreCase = true)) {
                status.setText(R.string.pick_folder_supernote)
                pickFolder(saveAfter = true)
            } else {
                file?.let(::save)
            }
        }
        findViewById<View>(R.id.folder).setOnClickListener { pickFolder(saveAfter = false) }

        status.setText(R.string.loading)
        app.io.execute {
            val full = if (id != null) app.wallpapers.full(id) else app.member.card(intent.getBooleanExtra(EXTRA_CARD_DARK, false))
            val bitmap = full?.let { BitmapFactory.decodeFile(it.path) }
            val number = id?.let { app.wallpapers.memberNumber(it) }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                // "#042 · pseudo", like "Shared by" on the site.
                if (id != null) member.text = listOfNotNull(number, author).joinToString(" · ")
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

    /** Android's folder picker; the folder is kept for every later Save, on any device. */
    private fun pickFolder(saveAfter: Boolean) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        if (Build.MANUFACTURER.equals("Supernote", ignoreCase = true)) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:SCREENSHOT"))
        }
        try {
            startActivityForResult(intent, if (saveAfter) REQUEST_FOLDER_THEN_SAVE else REQUEST_FOLDER)
        } catch (e: ActivityNotFoundException) {
            status.setText(R.string.no_folder_picker)
            if (saveAfter) file?.let(::save)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val tree = data?.data
        if (resultCode != RESULT_OK || tree == null) {
            status.text = "" // cancelled: nothing saved, the next Save asks again
            return
        }
        contentResolver.takePersistableUriPermission(tree,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        app.prefs.wallpaperFolder = tree.toString()
        status.text = folderName(tree)
        if (requestCode == REQUEST_FOLDER_THEN_SAVE) file?.let(::save)
    }

    /** Copies the original file into the picked folder, else Pictures/ReadmeClub through MediaStore (no permission needed on Android 11). */
    private fun save(file: File) {
        val mime = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            .also { BitmapFactory.decodeFile(file.path, it) }.outMimeType ?: "image/jpeg"
        val name = "readmeclub-${file.name}.${mime.substringAfter('/')}"
        val tree = app.prefs.wallpaperFolder?.let(Uri::parse)
        try {
            val uri = if (tree != null) {
                val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
                DocumentsContract.createDocument(contentResolver, parent, mime, name)
                    ?: error("the folder refused the file")
            } else {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, mime)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/ReadmeClub")
                }
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: error("MediaStore refused the file")
            }
            contentResolver.openOutputStream(uri)!!.use { out -> file.inputStream().use { it.copyTo(out) } }
            status.text = getString(R.string.wallpaper_saved, tree?.let(::folderName) ?: "Pictures/ReadmeClub")
        } catch (e: Exception) {
            status.text = getString(R.string.wallpaper_failed, e.message)
        }
    }

    /** "primary:SCREENSHOT" → "SCREENSHOT". */
    private fun folderName(tree: Uri) = DocumentsContract.getTreeDocumentId(tree).substringAfter(':').ifEmpty { "/" }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = keys.handle(event) || super.dispatchKeyEvent(event)

    companion object {
        /** The linked member's card (see MemberSync.card) instead of a gallery wallpaper. */
        private const val EXTRA_CARD_DARK = "card_dark"

        fun cardIntent(context: android.content.Context, dark: Boolean): Intent =
            Intent(context, WallpaperActivity::class.java)
                .putExtra(EXTRA_CARD_DARK, dark)
                .putExtra(EXTRA_TITLE, context.getString(R.string.member_card))
                .putExtra(EXTRA_AUTHOR, context.app.prefs.memberNumber)

        const val EXTRA_ID = "id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_AUTHOR = "author"
        private const val REQUEST_FOLDER = 1
        private const val REQUEST_FOLDER_THEN_SAVE = 2
    }
}
