package club.readme.android.game.stacks

import org.json.JSONArray
import org.json.JSONObject

enum class Stat { GUTS, WITS, LUCK, SAFE }

class Outcome(val text: String, val effects: List<String>)

class Choice(
    val label: String,
    val stat: Stat,
    val mod: Int,
    val success: Outcome,
    val failure: Outcome?,
    val critical: Outcome?,
    val fumble: Outcome?,
)

class Source(val author: String, val died: Int, val work: String, val published: Int)

class Room(
    val id: String, val kind: String, val weight: Int, val tiers: IntRange,
    val name: String, val text: String, val choices: List<Choice>, val source: Source?,
)

class Enemy(
    val id: String, val name: String, val text: String, val weight: Int, val tiers: IntRange,
    val level: Int, val hp: Int, val atk: Int, val def: Int, val resolve: Int,
    val weakness: String, val loot: List<String>, val named: Boolean,
    val specialName: String?, val specialText: String?, val source: Source?,
)

class Item(
    val id: String, val name: String, val text: String, val weight: Int, val tiers: IntRange,
    val kind: String, val rarity: String, val price: Int, val effects: List<String>,
    val tags: List<String>, val keep: Boolean,
) {
    val trinket: Boolean get() = kind == "trinket"
}

class Line(val trigger: String, val text: String)

class Achievement(val id: String, val name: String, val text: String, val counter: String, val at: Int)

class Biome(val tier: Int, val name: String, val text: String)

/**
 * The game's tables (see docs/games/stacks-content.md), parsed from JSON. [validate] checks
 * every rule of the content guide; a unit test runs it over the shipped files.
 */
class StacksContent(
    val rooms: List<Room>,
    val enemies: List<Enemy>,
    val items: List<Item>,
    val lines: List<Line>,
    val achievements: List<Achievement>,
    val biomes: List<Biome>,
    val modifiers: List<String>,
) {
    private val enemyById = enemies.associateBy { it.id }
    private val itemById = items.associateBy { it.id }
    private val roomById = rooms.associateBy { it.id }

    fun enemy(id: String): Enemy? = enemyById[id]
    fun item(id: String): Item? = itemById[id]
    fun room(id: String): Room? = roomById[id]

    /** Every broken rule, as readable messages; empty when the content is sound. */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        fun check(ok: Boolean, message: () -> String) {
            if (!ok) errors += message()
        }
        fun ids(kind: String, list: List<String>) {
            list.groupBy { it }.filter { it.value.size > 1 }.keys.forEach { errors += "$kind: duplicate id $it" }
            list.filterNot { ID.matches(it) }.forEach { errors += "$kind: bad id $it" }
        }
        fun source(where: String, s: Source?) {
            if (s != null) check(s.died < 1955 && s.published < 1930) { "$where: ${s.author} / ${s.work} is not public domain" }
        }
        fun effects(where: String, list: List<String>) {
            for (e in list) check(effectValid(e)) { "$where: bad effect $e" }
        }
        val tiers = 1..maxOf(1, biomes.size)
        ids("room", rooms.map { it.id })
        ids("enemy", enemies.map { it.id })
        ids("item", items.map { it.id })
        ids("achievement", achievements.map { it.id })
        for (r in rooms) {
            val w = "room ${r.id}"
            check(r.kind in KINDS) { "$w: kind ${r.kind}" }
            check(r.name.length <= 32) { "$w: name too long" }
            check(r.text.length <= 220) { "$w: text too long" }
            check(r.weight in 1..5 && r.tiers.first in tiers && r.tiers.last in tiers) { "$w: weight or tiers" }
            check(if (r.kind == "shop") r.choices.isEmpty() else r.choices.size in 1..3) { "$w: choices" }
            check(r.kind != "author" || r.source != null) { "$w: author room without source" }
            source(w, r.source)
            for (c in r.choices) {
                check(c.label.length <= 28) { "$w: label too long: ${c.label}" }
                check(c.mod in -2..2) { "$w: mod" }
                check(c.stat == Stat.SAFE || c.failure != null) { "$w: ${c.label} has no failure" }
                for (o in listOfNotNull(c.success, c.failure, c.critical, c.fumble)) {
                    check(o.text.length <= 160) { "$w: outcome too long: ${o.text.take(30)}…" }
                    effects(w, o.effects)
                }
            }
            if (r.kind == "encounter") {
                val fights = r.choices.flatMap { c -> listOfNotNull(c.success, c.failure, c.critical, c.fumble) }
                    .any { o -> o.effects.any { it == "fight" || it.startsWith("fight:") } }
                check(fights) { "$w: encounter without a fight" }
            }
        }
        for (e in enemies) {
            val w = "enemy ${e.id}"
            check(e.name.length <= 32 && e.text.length <= 160 && e.weakness.length <= 16) { "$w: too long" }
            check(e.level in 1..10 && e.hp > 0 && e.atk > 0 && e.def > 0 && e.resolve > 0) { "$w: stats" }
            check(e.weight in 1..5 && e.tiers.first in tiers && e.tiers.last in tiers) { "$w: weight or tiers" }
            check(e.loot.size <= 3 && e.loot.all { it in itemById }) { "$w: loot" }
            check(!e.named || (e.specialName != null && e.specialName.length <= 24 && (e.specialText?.length ?: 999) <= 140)) { "$w: special" }
            check(items.any { e.weakness in it.tags }) { "$w: no item carries ${e.weakness}" }
            source(w, e.source)
        }
        for (i in items) {
            val w = "item ${i.id}"
            check(i.name.length <= 24 && i.text.length <= 100) { "$w: too long" }
            check(i.kind == "consumable" || i.kind == "trinket") { "$w: kind" }
            check(i.rarity in RARITY) { "$w: rarity" }
            check(i.price in 1..99 && i.weight in 1..5 && i.tiers.first in tiers && i.tiers.last in tiers) { "$w: price, weight or tiers" }
            effects(w, i.effects)
            check(!i.trinket || i.effects.all { it.startsWith("stat:") }) { "$w: trinkets only change stats" }
        }
        for (l in lines) {
            check(l.trigger in TRIGGERS) { "line: trigger ${l.trigger}" }
            check(l.text.length <= 140) { "line too long: ${l.text.take(30)}…" }
        }
        TRIGGERS.filter { t -> lines.none { it.trigger == t } }.forEach { errors += "no line for $it" }
        for (a in achievements) {
            val w = "achievement ${a.id}"
            check(a.name.length <= 32 && a.text.length <= 100) { "$w: too long" }
            check(a.counter == "none" || a.counter in COUNTERS || COUNTER_PREFIXES.any { a.counter.startsWith(it) }) { "$w: counter ${a.counter}" }
            check(a.at >= 1) { "$w: at" }
        }
        for (k in KINDS) check(rooms.any { it.kind == k }) { "no $k room" }
        for (t in tiers) {
            check(enemies.any { !it.named && t in it.tiers }) { "tier $t: no enemy" }
            check(enemies.any { it.named && t in it.tiers }) { "tier $t: no named enemy" }
        }
        return errors
    }

    private fun effectValid(e: String): Boolean {
        val m = EFFECT.matchEntire(e) ?: return false
        val (name, arg) = m.destructured
        return when (name) {
            "hp" -> SIGNED.matchEntire(arg)?.let { abs(it) in 1..10 } == true
            "heal" -> arg == "half" || arg == "full"
            "xp" -> arg.removePrefix("+").toIntOrNull() in 1..100
            "coins" -> SIGNED.matchEntire(arg)?.let { abs(it) in 1..50 } == true
            "item" -> arg == "random" || arg in itemById
            "lose_item" -> arg.isEmpty()
            "stat" -> STAT.matches(arg)
            "goto" -> arg == "previous" || arg == "stairs"
            "fight" -> arg.isEmpty() || arg in enemyById
            "damage" -> arg.toIntOrNull() in 1..20
            "achievement" -> achievements.any { it.id == arg }
            "flag" -> FLAG.matches(arg)
            else -> false
        }
    }

    private fun abs(m: MatchResult): Int = kotlin.math.abs(m.value.toInt())

    companion object {
        val KINDS = listOf("encounter", "event", "author", "loot", "rest", "shop")
        val RARITY = listOf("common", "odd", "rare", "absurd")
        val TRIGGERS = listOf(
            "intro", "enter_floor", "room", "critical", "fumble", "flee", "death",
            "level_up", "achievement", "boss", "shop", "idle_return",
        )
        val COUNTERS = listOf(
            "deaths", "floor", "level", "crits", "fumbles", "flees", "kills", "bosses",
            "talked_down", "items_used", "author_rooms", "rests", "shops", "coins_spent",
        )
        val COUNTER_PREFIXES = listOf("kill:", "talk:", "room:", "flag:")

        private val ID = Regex("[a-z0-9-]{1,48}")
        private val EFFECT = Regex("([a-z_]+)(?::(.*))?")
        private val SIGNED = Regex("[+-]?[0-9]{1,3}")
        private val STAT = Regex("(GUTS|WITS|LUCK):[+-][12]")
        private val FLAG = Regex("[a-z0-9_-]{1,32}")

        /** The tables by file name: rooms, enemies, items, index, achievements, biomes. */
        fun parse(files: Map<String, String>): StacksContent {
            fun array(name: String): JSONArray = JSONArray(files.getValue(name))
            fun <T> JSONArray.map(f: (JSONObject) -> T): List<T> = (0 until length()).map { f(getJSONObject(it)) }
            fun JSONObject.strings(key: String): List<String> =
                optJSONArray(key)?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty()
            fun JSONObject.text(key: String): String? = if (isNull(key)) null else optString(key).ifEmpty { null }
            fun JSONObject.tiers(): IntRange = getJSONArray("tiers").let { it.getInt(0)..it.getInt(1) }
            fun JSONObject.source(): Source? = optJSONObject("source")?.let {
                Source(it.getString("author"), it.getInt("died"), it.getString("work"), it.getInt("published"))
            }
            fun JSONObject.outcome(key: String): Outcome? = optJSONObject(key)?.let { Outcome(it.getString("text"), it.strings("effects")) }

            val rooms = array("rooms").map { o ->
                Room(
                    o.getString("id"), o.getString("kind"), o.optInt("weight", 3), o.tiers(),
                    o.getString("name"), o.getString("text"),
                    (o.optJSONArray("choices") ?: JSONArray()).map { c ->
                        Choice(
                            c.getString("label"), Stat.valueOf(c.getString("stat")), c.optInt("mod"),
                            c.outcome("success")!!, c.outcome("failure"), c.outcome("critical"), c.outcome("fumble"),
                        )
                    },
                    o.source(),
                )
            }
            val enemies = array("enemies").map { o ->
                val special = o.optJSONObject("special")
                Enemy(
                    o.getString("id"), o.getString("name"), o.getString("text"), o.optInt("weight", 3), o.tiers(),
                    o.getInt("level"), o.getInt("hp"), o.getInt("atk"), o.getInt("def"), o.getInt("resolve"),
                    o.getString("weakness"), o.strings("loot"), o.optBoolean("named"),
                    special?.text("name"), special?.text("text"), o.source(),
                )
            }
            val items = array("items").map { o ->
                Item(
                    o.getString("id"), o.getString("name"), o.getString("text"), o.optInt("weight", 3), o.tiers(),
                    o.getString("kind"), o.getString("rarity"), o.getInt("price"), o.strings("effects"),
                    o.strings("tags"), o.optBoolean("keep"),
                )
            }
            val lines = JSONObject(files.getValue("index")).getJSONArray("lines").map { Line(it.getString("trigger"), it.getString("text")) }
            val achievements = array("achievements").map { o ->
                Achievement(o.getString("id"), o.getString("name"), o.getString("text"), o.getString("counter"), o.getInt("at"))
            }
            val b = JSONObject(files.getValue("biomes"))
            val biomes = b.getJSONArray("biomes").map { Biome(it.getInt("tier"), it.getString("name"), it.getString("text")) }.sortedBy { it.tier }
            val modifiers = b.getJSONArray("modifiers").let { a -> (0 until a.length()).map { a.getString(it) } }
            return StacksContent(rooms, enemies, items, lines, achievements, biomes, modifiers)
        }

        val FILES = listOf("rooms", "enemies", "items", "index", "achievements", "biomes")
    }
}
