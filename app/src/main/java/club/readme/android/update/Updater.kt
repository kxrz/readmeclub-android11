package club.readme.android.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import club.readme.android.BuildConfig
import club.readme.android.Versions
import club.readme.android.sync.Http
import club.readme.android.sync.string
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * In-app updates from GitHub Releases: read the manifest the CI attaches to the latest
 * release, download the APK, check its SHA-256, and hand it to Android's installer
 * (which always asks the user to confirm; a sideloaded app can't update silently).
 */
object Updater {

    private const val TAG = "Updater"

    // "latest" skips pre-releases (tags like v1.0.0-rc1), so test builds never show up as updates.
    private const val MANIFEST_URL = "https://github.com/kxrz/readmeclub-android11/releases/latest/download/manifest.json"

    data class Release(val version: String, val apkUrl: String, val sha256: String?) {
        val isNewer: Boolean get() = Versions.compare(version, BuildConfig.VERSION_NAME) > 0

        /**
         * Only release builds can update themselves: Android installs an update only when it is
         * signed with the same key, and debug builds are a separate app (.debug) anyway.
         * Releases published before the manifest carried a checksum can't be verified, so they're skipped.
         */
        val canInstall: Boolean get() = isNewer && !BuildConfig.DEBUG && sha256 != null
    }

    /** The latest published release, or null when offline or the manifest is unreadable. */
    fun latest(): Release? = try {
        val o = JSONObject(String(Http.get(MANIFEST_URL)))
        Release(o.getString("latestVersion"), o.getString("apkUrl"), o.string("sha256")?.lowercase())
    } catch (e: Exception) {
        Log.w(TAG, "Update check failed", e)
        null
    }

    /** Downloads the APK into the cache and checks it against the manifest's SHA-256. */
    fun download(context: Context, release: Release): File {
        val bytes = Http.get(release.apkUrl)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        check(digest == release.sha256) { "Checksum mismatch, update not installed" }
        val file = File(context.cacheDir, "update.apk")
        file.writeBytes(bytes)
        return file
    }

    /**
     * False when the user must first allow this app to install apps: Android's settings
     * screen for that is then opened, and the update can be retried from it.
     */
    fun ensureInstallAllowed(context: Context): Boolean {
        if (context.packageManager.canRequestPackageInstalls()) return true
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
        )
        return false
    }

    /** Starts the install; [InstallResultReceiver] then shows Android's confirmation screen. */
    fun install(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("readmeclub.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            // The installer adds the status extras to this intent, so it must stay mutable from Android 12 on.
            val mutable = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            val callback = PendingIntent.getBroadcast(
                context, sessionId,
                Intent(context, InstallResultReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or mutable,
            )
            session.commit(callback.intentSender)
        }
    }
}
