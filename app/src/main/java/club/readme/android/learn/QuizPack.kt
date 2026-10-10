package club.readme.android.learn

import org.json.JSONObject

/** A quiz question; [answers] holds the right one first (rounds shuffle them). */
class Question(val id: String, val theme: String, val text: String, val answers: List<String>, val why: String)

/**
 * A pack of questions, as JSON: {"id", "title", "version", "questions": [{"id", "theme", "q",
 * "a": [right, wrong, wrong, wrong], "why"}]}, plus "description" and "updatedAt" for
 * downloaded packs. The General knowledge pack ships in the APK; the others come from the
 * CMS catalogue (see PackSync).
 */
class QuizPack(
    val id: String,
    val title: String,
    val version: Int,
    val questions: List<Question>,
    val description: String = "",
    /** The CMS's updatedAt for a downloaded pack; empty for packs in the APK. */
    val updatedAt: String = "",
) {

    companion object {
        /** Limits that keep a question, its answers and the explanation on one S4 screen. */
        const val MAX_QUESTION = 110
        const val MAX_ANSWER = 30
        const val MAX_WHY = 160

        fun parse(json: String): QuizPack {
            val o = JSONObject(json)
            val items = o.getJSONArray("questions")
            val questions = (0 until items.length()).map { i ->
                val q = items.getJSONObject(i)
                val a = q.getJSONArray("a")
                require(a.length() == 4) { "Question ${q.optString("id")} needs 4 answers" }
                Question(
                    id = q.getString("id"),
                    theme = q.getString("theme"),
                    text = q.getString("q"),
                    answers = (0 until 4).map { a.getString(it) },
                    why = q.getString("why"),
                )
            }
            return QuizPack(o.getString("id"), o.getString("title"), o.optInt("version", 1), questions, o.optString("description"), o.optString("updatedAt"))
        }
    }
}
