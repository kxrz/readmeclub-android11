package club.readme.android.learn

import android.content.Context
import java.io.File

/** Quiz packs (in the APK, or downloaded by PackSync) and the player's progress, on the device only. */
class LearnStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("learn", Context.MODE_PRIVATE)

    /** The packs in the APK, parsed once per process (the General pack is 50 KB of JSON). */
    val bundled: List<QuizPack>
        get() = bundledCache ?: context.assets.list(PACK_DIR).orEmpty().sorted().map { name ->
            QuizPack.parse(context.assets.open("$PACK_DIR/$name").bufferedReader().use { it.readText() })
        }.also { bundledCache = it }

    /** Where PackSync stores downloaded packs. */
    val downloadDir = File(context.filesDir, PACK_DIR)

    /** Downloaded packs, read from disk each time (a few small files); unreadable ones are skipped. */
    fun downloaded(): List<QuizPack> =
        downloadDir.listFiles { f -> f.name.endsWith(".json") }.orEmpty()
            .mapNotNull { runCatching { QuizPack.parse(it.readText()) }.getOrNull() }
            .filter { p -> bundled.none { it.id == p.id } }
            .sortedBy { it.title }

    /** How many packs were downloaded, without reading them. */
    fun downloadedCount(): Int = downloadDir.listFiles { f -> f.name.endsWith(".json") }?.size ?: 0

    val packs: List<QuizPack> get() = bundled + downloaded()

    fun pack(id: String): QuizPack? = bundled.firstOrNull { it.id == id }
        ?: File(downloadDir, "$id.json").takeIf { it.exists() }?.let { runCatching { QuizPack.parse(it.readText()) }.getOrNull() }

    private val all by lazy { packs }

    /** A question from any installed pack (packs as they were on first call). */
    fun question(id: String): Question? = all.firstNotNullOfOrNull { p -> p.questions.firstOrNull { it.id == id } }

    fun seen(pack: String): Set<String> = prefs.getStringSet("seen/$pack", null).orEmpty()

    /** Adds [ids] to the questions seen in [pack]; once every question was seen, starts over. */
    fun markSeen(pack: QuizPack, ids: Collection<String>) {
        // Only ids still in the pack count, so an edited pack does not reset too early.
        var seen = (seen(pack.id) + ids).intersect(pack.questions.map { it.id }.toSet())
        if (seen.size >= pack.questions.size) seen = emptySet()
        prefs.edit().putStringSet("seen/${pack.id}", seen).apply()
    }

    /** Best round score in [pack], -1 if none yet. */
    fun best(pack: String): Int = prefs.getInt("best/$pack", -1)

    /** Records [score] if it beats the best; true when it does. */
    fun offerBest(pack: String, score: Int): Boolean {
        if (score <= best(pack)) return false
        prefs.edit().putInt("best/$pack", score).apply()
        return true
    }

    fun deck(): Leitner = Leitner.decode(prefs.getString("deck", null))

    fun saveDeck(deck: Leitner) = prefs.edit().putString("deck", deck.encode()).apply()

    /** Forgets a removed pack: its progress, best score and its cards in the deck. */
    fun forget(pack: String) {
        prefs.edit().remove("seen/$pack").remove("best/$pack").apply()
        val deck = deck()
        deck.ids().filter { it.startsWith("$pack-") }.forEach(deck::remove)
        saveDeck(deck)
    }

    companion object {
        const val PACK_DIR = "packs"

        @Volatile private var bundledCache: List<QuizPack>? = null
    }
}
