package club.readme.android.sync

import org.json.JSONObject

/** Non-empty string value, or null. (optString turns a JSON null into the text "null".) */
fun JSONObject.string(key: String): String? = if (isNull(key)) null else optString(key).ifEmpty { null }

/** URL of a populated Payload upload: the named rendition if it exists, else the original. */
fun JSONObject.mediaUrl(key: String, size: String): String? {
    val media = optJSONObject(key) ?: return null
    return media.optJSONObject("sizes")?.optJSONObject(size)?.string("url") ?: media.string("url")
}

/** Name of the first populated relation in [key], or null. */
fun JSONObject.firstName(key: String): String? = optJSONArray(key)?.optJSONObject(0)?.string("name")
