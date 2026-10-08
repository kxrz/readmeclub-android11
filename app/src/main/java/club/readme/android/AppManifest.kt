package club.readme.android

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Reads the manifest the CI attaches to the latest GitHub release (latest version, APK URL). */
object AppManifest {

    // "latest" skips pre-releases (tags like v1.0.0-rc1), so test builds never show up as updates.
    private const val MANIFEST_URL = "https://github.com/kxrz/readmeclub-android11/releases/latest/download/manifest.json"

    /** Latest published version, or null when offline or the manifest is unreadable. */
    fun fetchLatestVersion(appVersion: String): String? = try {
        val connection = URL(MANIFEST_URL).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.setRequestProperty("X-App-Version", appVersion)
        try {
            if (connection.responseCode != 200) null
            else JSONObject(connection.inputStream.bufferedReader().readText()).getString("latestVersion")
        } finally {
            connection.disconnect()
        }
    } catch (e: Exception) {
        null
    }
}
