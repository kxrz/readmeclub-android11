package club.readme.android.game.stacks

/**
 * The rules of The Stacks (docs/games/the-stacks.md): floors, rooms, d6 checks, fights,
 * effects, levels, death and achievements. Pure Kotlin over [StacksState]; every call is
 * one player action and leaves the state ready to save and show.
 */
class StacksEngine(private val content: StacksContent, val state: StacksState, private val heroName: String = "Reader") {

    private val rng = Rng(state.seed)

    /** A choice as the screen shows it: label, the tag on its right, and whether it can be taken. */
    class ChoiceView(val label: String, val tag: String, val enabled: Boolean = true)

    // ---- Derived values -------------------------------------------------------------------

    fun stat(name: String): Int =
        (state.stats[name] ?: 0) + carried().filter { it.trinket }.sumOf { item -> item.effects.sumOf { statBonus(it, name) } }

    val maxHp: Int get() = 10 + 2 * stat("GUTS") + 2 * state.level
    val xpToNext: Int get() = 50 * state.level

    /** 0-based tier of five floors, the biome it uses (1-based) and the cycle past the last biome. */
    private val tierIndex: Int get() = (state.floor - 1) / FLOORS_PER_TIER
    val biomeTier: Int get() = tierIndex % maxOf(1, content.biomes.size) + 1
    private val cycle: Int get() = tierIndex / maxOf(1, content.biomes.size)
    val modifier: String? get() = if (cycle == 0 || content.modifiers.isEmpty()) null else content.modifiers[(cycle - 1) % content.modifiers.size]
    val biome: Biome? get() = content.biomes.firstOrNull { it.tier == biomeTier }

    val difficulty: Int get() = minOf(9, 4 + state.floor / 3) + minOf(cycle, 3)

    fun named(name: String): String = modifier?.let { "$it $name" } ?: name

    fun carried(): List<Item> = state.bag.mapNotNull(content::item)

    val room: Room? get() = state.rooms.getOrNull(state.position)?.let(content::room)
    val enemy: Enemy? get() = (state.fight?.enemyId ?: state.roomEnemy)?.let(content::enemy)

    fun choices(): List<ChoiceView> {
        val r = room ?: return emptyList()
        if (r.kind == "shop") {
            return state.shop.mapNotNull(content::item).map { ChoiceView(it.name, "${it.price}c", state.coins >= it.price) } +
                ChoiceView(LEAVE, "SAFE")
        }
        return r.choices.map { ChoiceView(fill(it.label), it.stat.name) }
    }

    fun roomText(): String = room?.let { fill(it.text) }.orEmpty()

    // ---- Actions ---------------------------------------------------------------------------

    fun start(heroClass: String) {
        val s = CLASSES.getValue(heroClass)
        state.heroClass = heroClass
        state.stats.putAll(s)
        state.level = 1
        state.xp = 0
        state.hp = maxHp
        state.coins = 0
        state.bag.clear()
        state.indexLine = line("intro")
        enterFloor(1, announce = false)
        save()
    }

    /** A room choice (or a shop purchase, or Leave). */
    fun choose(index: Int) {
        val r = room ?: return
        if (state.screen != Screen.ROOM) return
        clearNotes()
        if (r.kind == "shop") return shop(index)
        val c = r.choices.getOrNull(index) ?: return
        val pills = mutableListOf<String>()
        val text: String
        var outcome: Outcome
        state.critical = false
        if (c.stat == Stat.SAFE) {
            outcome = c.success
            text = fill(outcome.text)
        } else {
            val d = rng.d6()
            val value = stat(c.stat.name)
            val needed = difficulty + c.mod
            val total = d + value
            outcome = when {
                d == 6 -> c.critical ?: c.success
                d == 1 -> c.fumble ?: c.failure!!
                total >= needed -> c.success
                else -> c.failure!!
            }
            if (d == 6) {
                state.critical = true
                bump("crits")
                state.indexLine = line("critical")
                if (c.critical == null) pills += gainXp(5)
            }
            if (d == 1) {
                bump("fumbles")
                state.indexLine = line("fumble")
            }
            text = "You roll $d + ${c.stat.name} $value = $total (needs $needed). " + fill(outcome.text)
        }
        pills += apply(outcome.effects)
        showOutcome(text, pills)
    }

    /** Continue from an outcome, a cleared floor ("Go down") or death ("Try again"). */
    fun next() {
        clearNotes()
        when (state.screen) {
            Screen.OUTCOME -> resume()
            Screen.FLOOR_CLEARED -> enterFloor(state.floor + 1, announce = true)
            Screen.DEATH -> {
                state.hp = maxHp
                state.position = 0
                state.fight = null
                state.pendingFight = null
                state.pendingItem = null
                state.pendingLevelUps = 0
                enterRoom()
            }
            else -> return
        }
        save()
    }

    fun levelUp(statName: String) {
        if (state.screen != Screen.LEVEL_UP || statName !in state.stats) return
        state.stats[statName] = state.stats.getValue(statName) + 1
        state.level++
        state.pendingLevelUps--
        state.hp = maxHp
        state.indexLine = line("level_up")
        resume()
        save()
    }

    /** Bag full: drop the item in [slot] to take the new one, or -1 to leave the new one. */
    fun resolveBag(slot: Int) {
        val incoming = state.pendingItem ?: return
        if (slot in state.bag.indices) state.bag[slot] = incoming
        state.pendingItem = null
        if (state.hp > maxHp) state.hp = maxHp
        resume()
        save()
    }

    enum class Action { ATTACK, TALK, FLEE }

    fun fight(action: Action) {
        val f = state.fight ?: return
        val e = content.enemy(f.enemyId) ?: return
        if (state.screen != Screen.FIGHT) return
        clearNotes()
        val d = rng.d6()
        when (action) {
            Action.ATTACK -> {
                val guts = stat("GUTS")
                val total = d + guts
                val sure = carried().any { it.trinket && e.weakness in it.tags }
                val hit = d == 6 || (d != 1 && (sure || total >= f.def))
                if (hit) {
                    var damage = 1 + maxOf(0, total - f.def)
                    if (d == 6) {
                        damage *= 2
                        bump("crits")
                    }
                    f.hp -= damage
                    log(f, "You roll $d + GUTS $guts = $total vs DEF ${f.def}: $damage damage" + (if (d == 6) ", critical." else "."))
                } else {
                    if (d == 1) bump("fumbles")
                    log(f, "You roll $d + GUTS $guts = $total vs DEF ${f.def}: you miss.")
                }
            }
            Action.TALK -> {
                val wits = stat("WITS")
                val total = d + wits
                if (d == 6 || (d != 1 && total >= f.resolve)) {
                    f.resolve = maxOf(0, f.resolve - if (d == 6) 2 else 1)
                    log(f, "You roll $d + WITS $wits = $total: it wavers (resolve ${f.resolve}).")
                } else {
                    f.offended = true
                    log(f, "You roll $d + WITS $wits = $total: it is offended.")
                }
                if (f.resolve <= 0) return win(f, e, talked = true)
            }
            Action.FLEE -> {
                val luck = stat("LUCK")
                val total = d + luck
                val needed = 4 + f.level / 2
                if (d == 6 || (d != 1 && total >= needed)) {
                    bump("flees")
                    state.fight = null
                    state.indexLine = line("flee")
                    showOutcome("You roll $d + LUCK $luck = $total (needs $needed). You get away. No reward, but no funeral either.", emptyList())
                    return save()
                }
                log(f, "You roll $d + LUCK $luck = $total (needs $needed): you trip over a footnote.")
            }
        }
        if (f.hp <= 0) return win(f, e, talked = false)
        enemyTurn(f, e)
        save()
    }

    /** Uses a consumable from the bag; in a fight it takes the turn. Returns what happened. */
    fun use(slot: Int): List<String> {
        val item = state.bag.getOrNull(slot)?.let(content::item) ?: return emptyList()
        if (item.trinket) return emptyList()
        val f = state.fight
        if (f == null && item.effects.any { it.startsWith("damage:") }) return emptyList()
        state.bag.removeAt(slot)
        bump("items_used")
        val pills = mutableListOf<String>()
        for (effect in item.effects) {
            if (effect.startsWith("damage:") && f != null) {
                val e = content.enemy(f.enemyId)
                var damage = effect.substringAfter(':').toInt()
                if (e != null && e.weakness in item.tags) damage *= 2
                f.hp -= damage
                log(f, "${item.name}: $damage damage" + (if (e != null && e.weakness in item.tags) ", right where it hurts." else "."))
            } else {
                pills += apply(listOf(effect))
            }
        }
        if (f != null) {
            if (pills.isNotEmpty()) log(f, "${item.name}: ${pills.joinToString(", ")}.")
            val e = content.enemy(f.enemyId)
            if (e != null) {
                if (f.hp <= 0) win(f, e, talked = false) else enemyTurn(f, e)
            }
        } else if (state.pendingLevelUps > 0 && state.screen == Screen.ROOM) {
            state.screen = Screen.LEVEL_UP
        }
        checkAchievements()
        save()
        return pills.ifEmpty { listOf(item.name) }
    }

    fun drop(slot: Int) {
        if (slot !in state.bag.indices || state.fight != null) return
        state.bag.removeAt(slot)
        if (state.hp > maxHp) state.hp = maxHp
        save()
    }

    /** The app was reopened after a day or more. */
    fun welcomeBack(now: Long) {
        if (state.lastPlayed > 0 && now - state.lastPlayed > DAY_MS && state.screen != Screen.INTRO) {
            state.indexLine = line("idle_return")
        }
        state.lastPlayed = now
        save()
    }

    // ---- Flow --------------------------------------------------------------------------------

    private fun enterFloor(floor: Int, announce: Boolean) {
        state.floor = floor
        state.counters["floor"] = maxOf(state.count("floor"), floor)
        state.rooms.clear()
        state.rooms += generateFloor()
        state.position = 0
        if (announce) state.indexLine = line("enter_floor")
        checkAchievements()
        enterRoom()
    }

    private fun generateFloor(): List<String> {
        val tier = biomeTier
        val count = 6 + rng.int(3)
        val used = mutableListOf<String>()
        repeat(count) {
            val kind = rng.pick(KIND_WEIGHTS.keys.toList()) { KIND_WEIGHTS.getValue(it) }
            val pool = content.rooms.filter { tier in it.tiers && it.id !in used }
            val candidates = pool.filter { it.kind == kind }.ifEmpty { pool }
            rng.pick(candidates) { it.weight }?.let { used += it.id }
        }
        if (state.floor % FLOORS_PER_TIER == 0) used += BOSS
        return used
    }

    private fun enterRoom() {
        state.roomEnemy = null
        state.shop.clear()
        val id = state.rooms.getOrNull(state.position)
        if (id == null) {
            state.screen = Screen.FLOOR_CLEARED
            return
        }
        if (id == BOSS) {
            val boss = rng.pick(content.enemies.filter { it.named && biomeTier in it.tiers }) { it.weight }
                ?: content.enemies.filter { biomeTier in it.tiers }.maxByOrNull { it.level }
            if (boss == null) return advance()
            state.indexLine = line("boss")
            return startFight(boss.id)
        }
        val r = content.room(id) ?: return advance()
        when (r.kind) {
            "encounter" -> state.roomEnemy = rng.pick(content.enemies.filter { !it.named && biomeTier in it.tiers }) { it.weight }?.id
            "shop" -> {
                val stock = content.items.filter { biomeTier in it.tiers }.toMutableList()
                repeat(3) { rng.pick(stock) { itemWeight(it) }?.let { stock.remove(it); state.shop += it.id } }
                bump("shops")
                state.indexLine = line("shop")
            }
            "rest" -> bump("rests")
            "author" -> bump("author_rooms")
        }
        bump("room:${r.id}")
        if (state.indexLine == null && rng.int(3) == 0) state.indexLine = line("room")
        state.screen = Screen.ROOM
        checkAchievements()
    }

    private fun advance() {
        state.position++
        enterRoom()
    }

    /** After an outcome: death, level-ups, a full bag, a fight, then the next room. */
    private fun resume() {
        when {
            state.hp <= 0 -> die()
            state.pendingLevelUps > 0 -> state.screen = Screen.LEVEL_UP
            state.pendingItem != null -> state.screen = Screen.BAG_FULL
            state.pendingFight != null -> {
                val id = state.pendingFight!!
                state.pendingFight = null
                startFight(id)
            }
            state.stay -> {
                state.stay = false
                state.screen = Screen.ROOM
            }
            else -> advance()
        }
    }

    private fun showOutcome(text: String, pills: List<String>) {
        state.outcomeText = text
        state.outcomePills.clear()
        state.outcomePills += pills
        state.screen = Screen.OUTCOME
        checkAchievements()
        save()
    }

    private fun shop(index: Int) {
        val item = state.shop.getOrNull(index)?.let(content::item)
        if (item == null) {
            showOutcome("You leave without buying anything. The till sulks.", emptyList())
            return
        }
        if (state.coins < item.price) return
        state.coins -= item.price
        bump("coins_spent", item.price)
        state.shop.remove(item.id)
        val pills = mutableListOf("−${item.price} coins") + give(item)
        state.stay = true
        showOutcome("You buy ${item.name}. ${item.text}", pills)
    }

    private fun startFight(enemyId: String) {
        val e = content.enemy(enemyId) ?: return advance()
        val scale = 1 + 0.15 * (state.floor - 1)
        fun scaled(v: Int) = maxOf(1, Math.round(v * scale).toInt())
        val hp = scaled(e.hp) * if (e.named) 3 else 1
        state.fight = FightState(e.id, e.named, e.level, hp, hp, scaled(e.atk), scaled(e.def), scaled(e.resolve))
        state.roomEnemy = e.id
        state.screen = Screen.FIGHT
    }

    private fun enemyTurn(f: FightState, e: Enemy) {
        f.turn++
        val guts = stat("GUTS")
        val name = named(e.name)
        if (f.named && f.turn % 3 == 0) {
            val damage = f.atk * 2
            state.hp -= damage
            log(f, "${e.specialName ?: "A special move"}: ${e.specialText ?: "it hits hard."} You lose $damage HP.")
        } else {
            val d = rng.d6()
            if (d == 6 || (d != 1 && d + f.atk >= 4 + guts)) {
                var damage = f.atk + if (f.offended) 1 else 0
                if (d == 6) damage *= 2
                f.offended = false
                state.hp -= damage
                log(f, "$name rolls $d: you lose $damage HP.")
            } else {
                log(f, "$name rolls $d and misses.")
            }
        }
        if (state.hp <= 0) die()
    }

    private fun win(f: FightState, e: Enemy, talked: Boolean) {
        state.fight = null
        val pills = mutableListOf<String>()
        pills += gainXp(10 * e.level)
        val coins = rng.d6() * e.level
        state.coins += coins
        pills += "+$coins coins"
        if (e.loot.isNotEmpty() && (e.named || rng.chance(50))) {
            rng.pick(e.loot)?.let(content::item)?.let { pills += give(it) }
        }
        if (talked) {
            bump("talked_down")
            bump("talk:${e.id}")
        } else {
            bump("kills")
            bump("kill:${e.id}")
        }
        if (e.named) bump("bosses")
        val name = named(e.name)
        val text = if (talked) "$name agrees it was all a misunderstanding and leaves, dropping its things."
        else "$name is defeated. It will be shelved under Fiction."
        showOutcome(text, pills)
    }

    private fun die() {
        state.fight = null
        state.hp = 0
        bump("deaths")
        val lost = state.coins / 2
        state.coins -= lost
        val losable = state.bag.indices.filter { content.item(state.bag[it])?.keep != true }
        val dropped = rng.pick(losable)?.let { state.bag.removeAt(it) }?.let(content::item)
        state.outcomePills.clear()
        if (lost > 0) state.outcomePills += "−$lost coins"
        if (dropped != null) state.outcomePills += "Lost: ${dropped.name}"
        state.indexLine = line("death")
        state.screen = Screen.DEATH
        checkAchievements()
    }

    // ---- Effects -----------------------------------------------------------------------------

    private fun apply(effects: List<String>): List<String> {
        val pills = mutableListOf<String>()
        for (effect in effects) {
            val name = effect.substringBefore(':')
            val arg = effect.substringAfter(':', "")
            when (name) {
                "hp" -> {
                    val n = arg.toInt()
                    state.hp = (state.hp + n).coerceIn(0, maxHp)
                    pills += if (n >= 0) "+$n HP" else "−${-n} HP"
                }
                "heal" -> {
                    val n = if (arg == "full") maxHp else maxHp / 2
                    val before = state.hp
                    state.hp = minOf(maxHp, state.hp + n)
                    pills += "+${state.hp - before} HP"
                }
                "xp" -> pills += gainXp(arg.toInt())
                "coins" -> {
                    val n = arg.toInt()
                    val before = state.coins
                    state.coins = maxOf(0, state.coins + n)
                    val change = state.coins - before
                    if (change != 0) pills += if (change > 0) "+$change coins" else "−${-change} coins"
                }
                "item" -> {
                    val item = if (arg == "random") randomItem() else content.item(arg)
                    if (item != null) pills += give(item)
                }
                "lose_item" -> {
                    val losable = state.bag.indices.filter { content.item(state.bag[it])?.keep != true }
                    rng.pick(losable)?.let { state.bag.removeAt(it) }?.let(content::item)?.let { pills += "Lost: ${it.name}" }
                }
                "stat" -> {
                    val (s, n) = arg.split(':').let { it[0] to it[1].toInt() }
                    state.stats[s] = maxOf(0, (state.stats[s] ?: 0) + n)
                    pills += (if (n >= 0) "+$n " else "−${-n} ") + s
                }
                "goto" -> when (arg) {
                    "previous" -> if (state.position > 0) state.rooms.add(state.position + 1, state.rooms[state.position - 1])
                    "stairs" -> {
                        // Drop the rooms left before the stairs, but never the boss.
                        val boss = state.rooms.indexOf(BOSS)
                        val end = if (boss > state.position) boss else state.rooms.size
                        repeat(end - state.position - 1) { state.rooms.removeAt(state.position + 1) }
                    }
                }
                "fight" -> state.pendingFight = arg.ifEmpty { state.roomEnemy }
                "achievement" -> unlock(arg)
                "flag" -> bump("flag:$arg")
            }
        }
        if (state.hp > maxHp) state.hp = maxHp
        return pills
    }

    private fun gainXp(n: Int): String {
        state.xp += n
        while (state.xp >= 50 * (state.level + state.pendingLevelUps)) {
            state.xp -= 50 * (state.level + state.pendingLevelUps)
            state.pendingLevelUps++
        }
        return "+$n XP"
    }

    private fun give(item: Item): String {
        if (state.bag.size < BAG) state.bag += item.id else state.pendingItem = item.id
        return item.name
    }

    private fun randomItem(): Item? = rng.pick(content.items.filter { biomeTier in it.tiers }) { itemWeight(it) }

    private fun itemWeight(item: Item): Int = item.weight * (RARITY_WEIGHT[item.rarity] ?: 1)

    private fun statBonus(effect: String, name: String): Int {
        val parts = effect.split(':')
        return if (parts.size == 3 && parts[0] == "stat" && parts[1] == name) parts[2].toInt() else 0
    }

    // ---- Narrator, counters, achievements ------------------------------------------------

    private fun line(trigger: String): String? {
        val all = content.lines.filter { it.trigger == trigger }
        val last = state.lastLines[trigger]
        val chosen = rng.pick(all.filter { it.text != last }.ifEmpty { all }) ?: return null
        state.lastLines[trigger] = chosen.text
        return fill(chosen.text)
    }

    private fun bump(key: String, by: Int = 1) {
        state.counters[key] = state.count(key) + by
    }

    private fun unlock(id: String) {
        if (id in state.achievements || content.achievements.none { it.id == id }) return
        state.achievements += id
        state.newAchievement = id
        state.indexLine = line("achievement")
    }

    private fun checkAchievements() {
        state.counters["level"] = state.level
        for (a in content.achievements) {
            if (a.counter != "none" && a.id !in state.achievements && state.count(a.counter) >= a.at) unlock(a.id)
        }
    }

    private fun clearNotes() {
        state.indexLine = null
        state.newAchievement = null
    }

    private fun log(f: FightState, line: String) {
        f.log += line
        while (f.log.size > 2) f.log.removeAt(0)
    }

    fun fill(text: String): String = text
        .replace("{hero}", heroName)
        .replace("{floor}", state.floor.toString())
        .replace("{enemy}", enemy?.let { named(it.name) } ?: "something")
        .replace("{item}", state.bag.lastOrNull()?.let(content::item)?.name ?: "something")

    /** Keeps the generator's position in the state, so saving the state saves the run. */
    private fun save() {
        state.seed = rng.state
    }

    companion object {
        const val BAG = 6
        const val FLOORS_PER_TIER = 5
        const val BOSS = "#boss"
        const val LEAVE = "Leave"
        private const val DAY_MS = 24 * 60 * 60 * 1000L

        val CLASSES = mapOf(
            "Margin Scribbler" to mapOf("WITS" to 3, "GUTS" to 1, "LUCK" to 2),
            "Spine Breaker" to mapOf("GUTS" to 3, "WITS" to 1, "LUCK" to 2),
            "Speed Reader" to mapOf("LUCK" to 3, "GUTS" to 2, "WITS" to 1),
        )
        private val KIND_WEIGHTS = mapOf("encounter" to 35, "event" to 30, "author" to 10, "loot" to 10, "rest" to 10, "shop" to 5)
        private val RARITY_WEIGHT = mapOf("common" to 60, "odd" to 25, "rare" to 12, "absurd" to 3)
    }
}
