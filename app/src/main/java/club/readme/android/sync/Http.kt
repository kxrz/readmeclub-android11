package club.readme.android.sync

import club.readme.android.BuildConfig
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

object Http {

    /** A response that is not an exception: [code] and [body], for calls that read errors. */
    class Response(val code: Int, val body: String)

    /** GET [url] and return the body, or throw on network error or non-200 status. */
    fun get(url: String, token: String? = null): ByteArray {
        val connection = open(url, token)
        try {
            check(connection.responseCode == 200) { "HTTP ${connection.responseCode} for $url" }
            return connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    /** [method] with a JSON [body]; throws only on network error, any status is returned. */
    fun send(method: String, url: String, body: String? = null, token: String? = null): Response {
        val connection = open(url, token)
        connection.requestMethod = method
        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toByteArray()) }
        }
        try {
            val code = connection.responseCode
            val stream = if (code < 400) connection.inputStream else connection.errorStream
            return Response(code, stream?.use { String(it.readBytes()) }.orEmpty())
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String, token: String?): HttpURLConnection {
        val connection = URL(encode(url)).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("X-App-Version", BuildConfig.VERSION_NAME)
        // The member account token (see MemberSync), only on readme.club's own API.
        if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
        return connection
    }

    /** Percent-encodes spaces and non-ASCII characters (e.g. from uploaded file names), which browsers do silently. */
    private fun encode(url: String): String = URI(url.replace(" ", "%20")).toASCIIString()
}
