package club.readme.android.sync

import android.os.Build
import android.util.Log
import club.readme.android.BuildConfig
import club.readme.android.data.Prefs
import org.json.JSONObject
import java.io.File

/**
 * The member account on readme.club: linking this reader (member number, the start of the
 * email, then the emailed code), the member's details, their card, and unlinking. The token
 * only reads (favourites, own card); the site lists and disconnects readers in Settings.
 */
class MemberSync(private val prefs: Prefs, private val cacheDir: File, private val screen: String) {

    class Member(val number: String, val name: String?, val favorites: Int)

    sealed class Result {
        object Ok : Result()
        class Failed(val message: String?) : Result()
        object Offline : Result()
    }

    val linked: Boolean get() = prefs.memberToken != null

    /** Step 1: the site answers the same whether or not the details match. */
    fun sendCode(number: String, emailStart: String): Result = call {
        Http.send("POST", "$SITE/api/app/link/send-code", JSONObject().put("member", number).put("local", emailStart).toString())
    }

    /** Step 2: the code from the email; on success this reader is linked. */
    fun verify(number: String, code: String): Result {
        val device = JSONObject()
            .put("manufacturer", Build.MANUFACTURER.take(60))
            .put("model", Build.MODEL.take(60))
            .put("android", Build.VERSION.RELEASE.take(20))
            .put("app", BuildConfig.VERSION_NAME.take(30))
            .put("screen", screen)
        val body = JSONObject().put("member", number).put("code", code).put("device", device).toString()
        return try {
            val response = Http.send("POST", "$SITE/api/app/link/verify", body)
            if (response.code != 200) return Result.Failed(error(response))
            val o = JSONObject(response.body)
            prefs.memberToken = o.getString("token")
            prefs.memberNumber = o.getString("member_id")
            Result.Ok
        } catch (e: Exception) {
            Log.w(TAG, "Link failed", e)
            Result.Offline
        }
    }

    /** The linked member, null when offline; a refused token unlinks this reader. */
    fun me(): Member? {
        val token = prefs.memberToken ?: return null
        return try {
            val response = Http.send("GET", "$SITE/api/app/me", token = token)
            if (response.code in REFUSED) {
                forget()
                return null
            }
            if (response.code != 200) return null
            val o = JSONObject(response.body)
            Member(o.getString("member_id"), o.string("display_name"), o.optInt("favorites"))
                .also { prefs.memberNumber = it.number }
        } catch (e: Exception) {
            Log.w(TAG, "Member details unavailable", e)
            null
        }
    }

    /** The member card as a PNG (light or [dark]), downloaded once per variant. */
    fun card(dark: Boolean): File? {
        val token = prefs.memberToken ?: return null
        val number = prefs.memberNumber?.removePrefix("#") ?: return null
        val file = File(cacheDir, "member/card-$number-${if (dark) "dark" else "light"}.png")
        return try {
            val bytes = Http.get("$SITE/api/member/card/$number.png" + if (dark) "?dark=true" else "", token)
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
            file
        } catch (e: Exception) {
            Log.w(TAG, "Card unavailable", e)
            file.takeIf { it.exists() }
        }
    }

    /**
     * Unlinks this reader here (always) and on the site; false when the site could not be
     * told (offline), so the member knows to disconnect it there too.
     */
    fun unlink(): Boolean {
        val token = prefs.memberToken ?: return true
        val told = runCatching { Http.send("DELETE", "$SITE/api/app/me", token = token).code }
            .getOrNull().let { it != null && (it == 200 || it in REFUSED) }
        forget()
        return told
    }

    /** Forgets the account here: token, number, card and cached favourites. */
    private fun forget() {
        prefs.memberToken = null
        prefs.memberNumber = null
        prefs.memberPending = null
        File(cacheDir, "member").deleteRecursively()
        File(cacheDir, "wallpapers/pages").listFiles { f -> f.name.startsWith(WallpaperSync.FAVORITES_PREFIX) }
            ?.forEach { it.delete() }
    }

    private fun call(request: () -> Http.Response): Result = try {
        val response = request()
        if (response.code == 200) Result.Ok else Result.Failed(error(response))
    } catch (e: Exception) {
        Log.w(TAG, "Member request failed", e)
        Result.Offline
    }

    private fun error(response: Http.Response): String? =
        runCatching { JSONObject(response.body).string("error") }.getOrNull()

    private companion object {
        const val TAG = "MemberSync"
        const val SITE = WallpaperSync.SITE
        /** What the site answers for a token it no longer knows. */
        val REFUSED = listOf(401, 403, 410)
    }
}
