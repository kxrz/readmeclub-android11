package club.readme.android.sync

import club.readme.android.BuildConfig
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

object Http {

    /** GET [url] and return the body, or throw on network error or non-200 status. */
    fun get(url: String): ByteArray {
        val connection = URL(encode(url)).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("X-App-Version", BuildConfig.VERSION_NAME)
        try {
            check(connection.responseCode == 200) { "HTTP ${connection.responseCode} for $url" }
            return connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    /** Percent-encodes spaces and non-ASCII characters (e.g. from uploaded file names), which browsers do silently. */
    private fun encode(url: String): String = URI(url.replace(" ", "%20")).toASCIIString()
}
