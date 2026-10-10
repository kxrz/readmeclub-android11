package club.readme.android.game.stacks

import org.json.JSONArray
import org.json.JSONObject

/** What the player is looking at; the engine moves between them. */
enum class Screen { INTRO, ROOM, OUTCOME, FIGHT, LEVEL_UP, BAG_FULL, FLOOR_CLEARED, DEATH }

/** A fight in progress, with the enemy's stats already scaled to the floor. */
class FightState(
    val enemyId: String,
    val named: Boolean,
    val level: Int,
    var hp: Int,
    val maxHp: Int,
    val atk: Int,
    val def: Int,
    var resolve: Int,
    var turn: Int = 0,
    var offended: Boolean = false,
    /** The last two turns, one sentence each. */
    val log: MutableList<String> = mutableListOf(),
)

/**
 * The whole run, saved as one small JSON file after every action ([toJson], [fromJson]).
 * Kept on death: level, XP, stats, counters, achievements.
 */
class StacksState {
    var seed: Long = 0
    var screen = Screen.INTRO
    var heroClass = ""
    val stats = mutableMapOf("GUTS" to 0, "WITS" to 0, "LUCK" to 0)
    var level = 1
    var xp = 0
    var hp = 0
    var coins = 0
    val bag = mutableListOf<String>()
    var floor = 1
    val rooms = mutableListOf<String>()
    var position = 0
    var roomEnemy: String? = null
    val shop = mutableListOf<String>()
    var fight: FightState? = null

    // The outcome screen.
    var outcomeText = ""
    val outcomePills = mutableListOf<String>()
    var critical = false
    /** After the outcome, stay in this room (a shop after a purchase). */
    var stay = false

    // Waiting after the current outcome, in this order.
    var pendingLevelUps = 0
    var pendingItem: String? = null
    var pendingFight: String? = null

    val counters = mutableMapOf<String, Int>()
    val achievements = mutableListOf<String>()
    /** Shown as a sticker on the next screen, then cleared. */
    var newAchievement: String? = null
    var indexLine: String? = null
    val lastLines = mutableMapOf<String, String>()
    var lastPlayed = 0L

    fun count(key: String): Int = counters[key] ?: 0

    fun toJson(): String = JSONObject().apply {
        put("version", VERSION)
        put("seed", seed)
        put("screen", screen.name)
        put("heroClass", heroClass)
        put("stats", JSONObject(stats as Map<*, *>))
        put("level", level); put("xp", xp); put("hp", hp); put("coins", coins)
        put("bag", JSONArray(bag))
        put("floor", floor)
        put("rooms", JSONArray(rooms))
        put("position", position)
        put("roomEnemy", roomEnemy ?: "")
        put("shop", JSONArray(shop))
        fight?.let { f ->
            put("fight", JSONObject().apply {
                put("enemyId", f.enemyId); put("named", f.named); put("level", f.level)
                put("hp", f.hp); put("maxHp", f.maxHp); put("atk", f.atk); put("def", f.def)
                put("resolve", f.resolve); put("turn", f.turn); put("offended", f.offended)
                put("log", JSONArray(f.log))
            })
        }
        put("outcomeText", outcomeText)
        put("outcomePills", JSONArray(outcomePills))
        put("critical", critical); put("stay", stay)
        put("pendingLevelUps", pendingLevelUps)
        put("pendingItem", pendingItem ?: "")
        put("pendingFight", pendingFight ?: "")
        put("counters", JSONObject(counters as Map<*, *>))
        put("achievements", JSONArray(achievements))
        put("newAchievement", newAchievement ?: "")
        put("indexLine", indexLine ?: "")
        put("lastLines", JSONObject(lastLines as Map<*, *>))
        put("lastPlayed", lastPlayed)
    }.toString()

    companion object {
        const val VERSION = 1

        /** The saved run, or null when [json] is unreadable or from an unknown version. */
        fun fromJson(json: String): StacksState? = runCatching {
            val o = JSONObject(json)
            require(o.getInt("version") == VERSION)
            fun JSONObject.list(key: String): List<String> = getJSONArray(key).let { a -> (0 until a.length()).map { a.getString(it) } }
            fun JSONObject.ints(key: String): Map<String, Int> = getJSONObject(key).let { m -> m.keys().asSequence().associateWith { m.getInt(it) } }
            fun JSONObject.nullable(key: String): String? = optString(key).ifEmpty { null }
            StacksState().apply {
                seed = o.getLong("seed")
                screen = Screen.valueOf(o.getString("screen"))
                heroClass = o.getString("heroClass")
                stats.putAll(o.ints("stats"))
                level = o.getInt("level"); xp = o.getInt("xp"); hp = o.getInt("hp"); coins = o.getInt("coins")
                bag += o.list("bag")
                floor = o.getInt("floor")
                rooms += o.list("rooms")
                position = o.getInt("position")
                roomEnemy = o.nullable("roomEnemy")
                shop += o.list("shop")
                fight = o.optJSONObject("fight")?.let { f ->
                    FightState(
                        f.getString("enemyId"), f.getBoolean("named"), f.getInt("level"), f.getInt("hp"), f.getInt("maxHp"),
                        f.getInt("atk"), f.getInt("def"), f.getInt("resolve"), f.getInt("turn"), f.getBoolean("offended"),
                        f.list("log").toMutableList(),
                    )
                }
                outcomeText = o.getString("outcomeText")
                outcomePills += o.list("outcomePills")
                critical = o.getBoolean("critical"); stay = o.getBoolean("stay")
                pendingLevelUps = o.getInt("pendingLevelUps")
                pendingItem = o.nullable("pendingItem")
                pendingFight = o.nullable("pendingFight")
                counters.putAll(o.ints("counters"))
                achievements += o.list("achievements")
                newAchievement = o.nullable("newAchievement")
                indexLine = o.nullable("indexLine")
                o.getJSONObject("lastLines").let { m -> m.keys().forEach { lastLines[it] = m.getString(it) } }
                lastPlayed = o.getLong("lastPlayed")
            }
        }.getOrNull()
    }
}
