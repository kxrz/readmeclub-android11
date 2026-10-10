package club.readme.android.ui.member

import android.app.Activity
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import club.readme.android.R
import club.readme.android.app
import club.readme.android.sync.MemberSync
import club.readme.android.sync.WallpaperSync
import club.readme.android.ui.wallpapers.WallpaperActivity

/**
 * Member: link this reader to a readme.club account with the member number and the start of
 * the email, then the emailed code. Once linked: the member's favourites and uploads, the
 * member card as a wallpaper, and Unlink. The link only reads; the site lists and
 * disconnects linked readers in its account settings.
 */
class MemberTab(
    private val activity: Activity,
    container: ViewGroup,
    private val onWallpapers: (WallpaperSync.Scope, Int) -> Unit,
    private val onHome: () -> Unit,
) {

    private enum class Step { DETAILS, CODE, LINKED }

    private val root: View = activity.layoutInflater.inflate(R.layout.member, container, true)
    private val text: TextView = root.findViewById(R.id.member_text)
    private val field1: EditText = root.findViewById(R.id.field1)
    private val field2: EditText = root.findViewById(R.id.field2)
    private val status: TextView = root.findViewById(R.id.member_status)
    private val primary: TextView = root.findViewById(R.id.primary)
    private val unlink: TextView = root.findViewById(R.id.unlink)
    private val member = activity.app.member

    private var step = if (member.linked) Step.LINKED else Step.DETAILS
    private var number = ""
    private var busy = false
    private var confirmUnlink = false

    init {
        root.findViewById<View>(R.id.back).setOnClickListener { back() }
        primary.setOnClickListener { primaryAction() }
        root.findViewById<View>(R.id.favorites).setOnClickListener { onWallpapers(WallpaperSync.Scope.Favorites, R.string.member_favorites_title) }
        root.findViewById<View>(R.id.uploads).setOnClickListener {
            activity.app.prefs.memberNumber?.let { onWallpapers(WallpaperSync.Scope.Uploads(it), R.string.member_uploads) }
        }
        root.findViewById<View>(R.id.card).setOnClickListener {
            activity.startActivity(WallpaperActivity.cardIntent(activity, dark = false))
        }
        unlink.setOnClickListener { unlink() }
        show()
        if (step == Step.LINKED) refresh()
    }

    /** Back: from the code to the details, else Home. */
    fun back() {
        if (step == Step.CODE && !busy) {
            step = Step.DETAILS
            show()
        } else {
            onHome()
        }
    }

    private fun show() {
        status.text = ""
        val linking = step != Step.LINKED
        root.findViewById<View>(R.id.link).visibility = if (linking) View.VISIBLE else View.GONE
        root.findViewById<View>(R.id.linked).visibility = if (linking) View.GONE else View.VISIBLE
        when (step) {
            Step.DETAILS -> {
                text.setText(R.string.member_intro)
                field(field1, R.id.field1_label, R.string.member_number, InputType.TYPE_CLASS_NUMBER, number)
                field(field2, R.id.field2_label, R.string.member_email_start, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, "")
                primary.setText(R.string.member_send_code)
            }
            Step.CODE -> {
                text.text = activity.getString(R.string.member_code_sent, number)
                field(field1, R.id.field1_label, R.string.member_code, InputType.TYPE_CLASS_NUMBER, "")
                field2.visibility = View.GONE
                root.findViewById<View>(R.id.field2_label).visibility = View.GONE
                primary.setText(R.string.member_link)
            }
            Step.LINKED -> {
                text.setText(R.string.member_linked_text)
                root.findViewById<TextView>(R.id.number).text = activity.app.prefs.memberNumber
                confirmUnlink = false
                unlink.setText(R.string.member_unlink)
                primary.setText(R.string.member_refresh)
                favorites(null)
            }
        }
    }

    private fun field(edit: EditText, label: Int, labelText: Int, type: Int, value: String) {
        edit.visibility = View.VISIBLE
        root.findViewById<TextView>(label).apply { setText(labelText); visibility = View.VISIBLE }
        edit.inputType = type
        edit.setText(value)
    }

    private fun favorites(count: Int?) {
        root.findViewById<TextView>(R.id.favorites).text =
            if (count == null) activity.getString(R.string.member_favorites_title)
            else activity.getString(R.string.member_favorites, count)
    }

    private fun primaryAction() {
        if (busy) return
        hideKeyboard()
        when (step) {
            Step.DETAILS -> {
                number = field1.text.toString().filter(Char::isDigit)
                val start = field2.text.toString().trim().substringBefore('@')
                if (number.isEmpty() || start.isEmpty()) return status.setText(R.string.member_fill_both)
                run(R.string.member_sending, { member.sendCode(number, start) }) {
                    step = Step.CODE
                    show()
                }
            }
            Step.CODE -> {
                val code = field1.text.toString().filter(Char::isDigit)
                if (code.length != 6) return status.setText(R.string.member_code_six)
                run(R.string.member_linking, { member.verify(number, code) }) {
                    step = Step.LINKED
                    show()
                    refresh()
                }
            }
            Step.LINKED -> refresh()
        }
    }

    /** Runs [call] off the main thread; [onOk] on success, else the site's message or "offline". */
    private fun run(working: Int, call: () -> MemberSync.Result, onOk: () -> Unit) {
        busy = true
        status.setText(working)
        activity.app.io.execute {
            val result = call()
            activity.runOnUiThread {
                busy = false
                when (result) {
                    MemberSync.Result.Ok -> onOk()
                    is MemberSync.Result.Failed -> status.text = result.message ?: activity.getString(R.string.member_failed)
                    MemberSync.Result.Offline -> status.setText(R.string.member_offline)
                }
            }
        }
    }

    private fun refresh() {
        status.setText(R.string.loading)
        activity.app.io.execute {
            val me = member.me()
            activity.runOnUiThread {
                if (!member.linked) {
                    // The site no longer knows this reader (disconnected from Settings, or idle too long).
                    step = Step.DETAILS
                    show()
                    status.setText(R.string.member_unlinked_remote)
                    return@runOnUiThread
                }
                if (me == null) {
                    status.setText(R.string.member_offline)
                    return@runOnUiThread
                }
                root.findViewById<TextView>(R.id.number).text = me.number
                root.findViewById<TextView>(R.id.name).text = me.name.orEmpty()
                favorites(me.favorites)
                status.setText(R.string.member_up_to_date)
            }
        }
    }

    /** Two taps: the first asks, the second unlinks. */
    private fun unlink() {
        if (!confirmUnlink) {
            confirmUnlink = true
            unlink.setText(R.string.member_unlink_confirm)
            return
        }
        busy = true
        activity.app.io.execute {
            member.unlink()
            activity.runOnUiThread {
                busy = false
                step = Step.DETAILS
                number = ""
                show()
                status.setText(R.string.member_unlinked)
            }
        }
    }

    private fun hideKeyboard() {
        activity.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(root.windowToken, 0)
    }
}
