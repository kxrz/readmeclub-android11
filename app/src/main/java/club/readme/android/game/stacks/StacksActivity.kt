package club.readme.android.game.stacks

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import club.readme.android.R
import club.readme.android.app
import club.readme.android.data.Prefs
import club.readme.android.eink.FullRefresh
import club.readme.android.eink.PageKeys
import java.io.File

/**
 * The Stacks: every screen of the game in one skeleton (layout/stacks.xml), drawn from the
 * engine's state after each tap. The run is saved after every action.
 */
class StacksActivity : Activity() {

    private enum class Overlay { NONE, HERO, ACHIEVEMENTS }

    private lateinit var content: StacksContent
    private lateinit var engine: StacksEngine
    private val saveFile by lazy { File(filesDir, SAVE) }

    private var overlay = Overlay.NONE
    private var fightBag = false
    private var slot = -1
    private var page = 0
    private var introClass: String? = null
    private var note: String? = null
    private var movesSinceRefresh = 0

    private val choices by lazy { findViewById<LinearLayout>(R.id.choices) }
    private val primary by lazy { findViewById<TextView>(R.id.primary) }
    private val previous by lazy { findViewById<TextView>(R.id.previous) }
    private val status by lazy { findViewById<TextView>(R.id.status) }

    // On outcome-like screens a page key continues; in a fight it never attacks by mistake.
    private val keys = PageKeys(onNext = { pageKey(1) }, onPrevious = { pageKey(-1) })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stacks)
        content = load(this)
        val saved = runCatching { saveFile.readText() }.getOrNull()
        val state = saved?.let { runCatching { StacksState.fromJson(it) }.getOrNull() }
            ?: StacksState().apply {
                seed = System.nanoTime()
                // A save this version cannot read is kept aside, never overwritten.
                if (saved != null) saveFile.renameTo(File(filesDir, "$SAVE.bak"))
            }
        engine = StacksEngine(content, state)
        findViewById<View>(R.id.back).setOnClickListener { back() }
        previous.setOnClickListener { pageKey(-1) }
        primary.setOnClickListener { primaryAction() }
    }

    override fun onResume() {
        super.onResume()
        engine.welcomeBack(System.currentTimeMillis())
        render()
    }

    override fun onPause() {
        super.onPause()
        engine.state.lastPlayed = System.currentTimeMillis()
        persist()
    }

    private fun persist() {
        runCatching {
            val tmp = File(filesDir, "$SAVE.tmp")
            tmp.writeText(engine.state.toJson())
            tmp.renameTo(saveFile)
        }
    }

    /** Runs a player action, saves, redraws (with a full refresh every few moves). */
    private fun act(force: Boolean = false, action: () -> Unit) {
        action()
        persist()
        render()
        val every = app.prefs.refreshEvery
        if (force || (every != Prefs.REFRESH_OFF && ++movesSinceRefresh >= every * 2)) {
            movesSinceRefresh = 0
            FullRefresh.flash(findViewById(R.id.flash), force)
        }
    }

    private fun back() {
        when {
            overlay != Overlay.NONE -> {
                overlay = Overlay.NONE
                slot = -1
                render()
            }
            else -> finish()
        }
    }

    private fun primaryAction() {
        val s = engine.state
        when {
            overlay == Overlay.HERO && fightBag -> back()
            overlay == Overlay.HERO -> {
                overlay = Overlay.ACHIEVEMENTS
                page = 0
                render()
            }
            overlay == Overlay.ACHIEVEMENTS -> pageKey(1)
            s.screen == Screen.INTRO -> introClass?.let { c -> act { engine.start(c) } }
            s.screen == Screen.ROOM -> {
                overlay = Overlay.HERO
                fightBag = false
                slot = -1
                render()
            }
            s.screen == Screen.OUTCOME || s.screen == Screen.DEATH -> act { engine.next() }
            s.screen == Screen.FLOOR_CLEARED -> act(force = true) { engine.next() }
        }
    }

    private fun pageKey(by: Int) {
        if (overlay == Overlay.ACHIEVEMENTS) {
            val pages = (content.achievements.size + PER_PAGE - 1) / PER_PAGE
            val target = page + by
            if (target in 0 until pages) {
                page = target
                render()
            }
            return
        }
        if (by > 0 && overlay == Overlay.NONE && engine.state.screen in listOf(Screen.OUTCOME, Screen.FLOOR_CLEARED, Screen.DEATH)) primaryAction()
    }

    // ---- Drawing ----------------------------------------------------------------------------

    private fun render() {
        reset()
        when (overlay) {
            Overlay.HERO -> return renderHero()
            Overlay.ACHIEVEMENTS -> return renderAchievements()
            Overlay.NONE -> Unit
        }
        val s = engine.state
        if (s.screen != Screen.INTRO) statusLine()
        index(s.indexLine)
        s.newAchievement?.let { id -> content.achievements.firstOrNull { it.id == id } }?.let { sticker(getString(R.string.stacks_achievement, it.name)) }
        when (s.screen) {
            Screen.INTRO -> renderIntro()
            Screen.ROOM -> renderRoom()
            Screen.OUTCOME -> renderOutcome()
            Screen.FIGHT -> renderFight()
            Screen.LEVEL_UP -> renderLevelUp()
            Screen.BAG_FULL -> renderBagFull()
            Screen.FLOOR_CLEARED -> renderFloorCleared()
            Screen.DEATH -> renderDeath()
        }
    }

    private fun reset() {
        choices.removeAllViews()
        for (id in listOf(R.id.sticker, R.id.index_box, R.id.card, R.id.pills, R.id.status_line)) findViewById<View>(id).visibility = View.GONE
        findViewById<TextView>(R.id.pill).visibility = View.GONE
        previous.visibility = View.GONE
        primary.visibility = View.GONE
        status.text = ""
        body("")
    }

    private fun renderIntro() {
        title(getString(R.string.stacks_title))
        index(engine.state.indexLine ?: getString(R.string.stacks_intro_default))
        body(getString(R.string.stacks_intro))
        for ((name, stats) in StacksEngine.CLASSES) {
            val best = stats.maxBy { it.value }
            row(name, "${best.key} ${best.value}", selected = name == introClass) {
                introClass = name
                render()
            }
        }
        primary(getString(R.string.stacks_start), enabled = introClass != null)
    }

    private fun renderRoom() {
        val room = engine.room ?: return
        title(engine.named(room.name))
        pill(getString(R.string.stacks_floor_room, engine.state.floor, engine.state.position + 1, engine.state.rooms.size))
        body(engine.roomText())
        // Four choices (a shop) fill the screen: the Index keeps quiet there.
        if (engine.choices().size >= 4) findViewById<View>(R.id.index_box).visibility = View.GONE
        engine.choices().forEachIndexed { i, c ->
            row(c.label, c.tag, enabled = c.enabled) { act { engine.choose(i) } }
        }
        status.setText(R.string.stacks_autosaved)
        primary(getString(R.string.stacks_hero_bag))
    }

    private fun renderOutcome() {
        val s = engine.state
        title(engine.room?.let { engine.named(it.name) } ?: getString(R.string.stacks_title))
        pill(getString(R.string.stacks_floor_room, s.floor, s.position + 1, s.rooms.size))
        if (s.critical) sticker(getString(R.string.stacks_critical))
        body(s.outcomeText)
        pills(s.outcomePills)
        primary(getString(R.string.stacks_continue))
    }

    private fun renderFight() {
        val f = engine.state.fight ?: return
        val e = engine.enemy ?: return
        title(getString(R.string.stacks_fight))
        pill(getString(R.string.stacks_turn, f.turn + 1))
        card(
            engine.named(e.name),
            getString(R.string.stacks_enemy_meta, f.level, e.weakness),
            "HP " + squares(f.hp, f.maxHp) + " ${maxOf(0, f.hp)}/${f.maxHp}",
        )
        body(if (f.log.isEmpty()) engine.fill(e.text) else f.log.joinToString("\n"))
        val canUse = engine.carried().any { !it.trinket }
        grid(
            listOf(
                Cell(getString(R.string.stacks_attack), "GUTS") { act { engine.fight(StacksEngine.Action.ATTACK) } },
                Cell(getString(R.string.stacks_talk), "WITS") { act { engine.fight(StacksEngine.Action.TALK) } },
                Cell(getString(R.string.stacks_use_item), "BAG", canUse) {
                    overlay = Overlay.HERO
                    fightBag = true
                    slot = -1
                    render()
                },
                Cell(getString(R.string.stacks_flee), "LUCK") { act { engine.fight(StacksEngine.Action.FLEE) } },
            ),
            columns = 2, heightDp = 56,
        )
        status.setText(R.string.stacks_pick_move)
    }

    private fun renderLevelUp() {
        title(getString(R.string.stacks_level_up, engine.state.level + 1))
        body(getString(R.string.stacks_level_up_text))
        for (stat in listOf("GUTS", "WITS", "LUCK")) {
            row(stat, engine.stat(stat).toString()) { act { engine.levelUp(stat) } }
        }
    }

    private fun renderBagFull() {
        val incoming = engine.state.pendingItem?.let(content::item) ?: return
        title(getString(R.string.stacks_bag_full))
        body(getString(R.string.stacks_bag_full_text, incoming.name, incoming.text))
        grid(engine.carried().mapIndexed { i, item -> Cell(item.name, null) { act { engine.resolveBag(i) } } }, columns = 3, heightDp = 56)
        row(getString(R.string.stacks_leave_it, incoming.name), "") { act { engine.resolveBag(-1) } }
    }

    private fun renderFloorCleared() {
        val s = engine.state
        title(getString(R.string.stacks_floor_cleared, s.floor))
        val next = s.floor + 1
        body(getString(R.string.stacks_floor_cleared_text, s.rooms.size, next))
        primary(getString(R.string.stacks_go_down))
    }

    private fun renderDeath() {
        val s = engine.state
        title(getString(R.string.stacks_died))
        body(getString(R.string.stacks_died_text, s.floor))
        pills(s.outcomePills)
        status.text = resources.getQuantityString(R.plurals.stacks_deaths, s.count("deaths"), s.count("deaths"))
        primary(getString(R.string.stacks_try_again))
    }

    private fun renderHero() {
        val s = engine.state
        title(getString(R.string.stacks_hero_name))
        pill("Lv ${s.level}")
        val xp = engine.xpToNext
        card(
            s.heroClass,
            "HP ${s.hp}/${engine.maxHp} · ${s.coins} coins · " +
                resources.getQuantityString(R.plurals.stacks_deaths, s.count("deaths"), s.count("deaths")),
            squares(s.xp, xp) + " " + getString(R.string.stacks_xp, s.xp, xp, s.level + 1),
        )
        statTiles()
        val bag = engine.carried()
        val item = bag.getOrNull(slot)
        label(getString(R.string.stacks_bag_label, bag.size, StacksEngine.BAG))
        grid(
            (0 until StacksEngine.BAG).map { i ->
                val it = bag.getOrNull(i)
                Cell(it?.name ?: "", null, enabled = it != null && !(fightBag && it.trinket), selected = i == slot) {
                    slot = i
                    render()
                }
            },
            columns = 3, heightDp = 56,
        )
        if (item != null) {
            body(item.text)
            val usable = !item.trinket && !(item.effects.any { it.startsWith("damage:") } && !fightBag)
            // Use and Drop side by side, under the bag.
            val actions = mutableListOf<Cell>()
            if (usable) actions += Cell(getString(R.string.stacks_use_short), null, textSp = 15f) {
                val pills = engine.use(slot)
                slot = -1
                if (fightBag) overlay = Overlay.NONE else note = pills.joinToString(" · ")
                act { }
            }
            if (!fightBag) actions += Cell(getString(R.string.stacks_drop_short), null, textSp = 15f) {
                engine.drop(slot)
                slot = -1
                act { }
            }
            if (actions.isNotEmpty()) grid(actions, columns = 2, heightDp = 52)
        }
        status.text = note ?: getString(R.string.stacks_best, s.count("floor"))
        note = null
        primary(getString(if (fightBag) R.string.stacks_back_to_fight else R.string.stacks_achievements))
    }

    private fun renderAchievements() {
        val all = content.achievements
        val pages = (all.size + PER_PAGE - 1) / PER_PAGE
        title(getString(R.string.stacks_achievements))
        pill("${engine.state.achievements.size} / ${all.size}")
        val text = all.drop(page * PER_PAGE).take(PER_PAGE).joinToString("\n\n") { a ->
            if (a.id in engine.state.achievements) "${a.name}\n${a.text}" else "???"
        }
        body(text)
        previous.visibility = View.VISIBLE
        previous.isEnabled = page > 0
        status.text = getString(R.string.page_of, page + 1, pages)
        if (page < pages - 1) primary(getString(R.string.next))
    }

    // ---- Pieces -------------------------------------------------------------------------------

    private fun title(text: String) {
        findViewById<TextView>(R.id.title).text = text
    }

    private fun pill(text: String) {
        findViewById<TextView>(R.id.pill).apply { this.text = text; visibility = View.VISIBLE }
    }

    private fun statusLine() {
        val s = engine.state
        findViewById<TextView>(R.id.status_line).apply {
            text = getString(R.string.stacks_status, s.hp, engine.maxHp, s.level, s.coins)
            visibility = View.VISIBLE
        }
    }

    private fun index(line: String?) {
        if (line == null) return
        findViewById<TextView>(R.id.index_text).text = line
        findViewById<View>(R.id.index_box).visibility = View.VISIBLE
    }

    /** A second sticker on the same screen (a critical that unlocks an achievement) joins the first. */
    private fun sticker(text: String) {
        findViewById<TextView>(R.id.sticker).apply {
            this.text = if (visibility == View.VISIBLE) "${this.text} · $text" else text
            visibility = View.VISIBLE
        }
    }

    private fun card(title: String, meta: String, bar: String) {
        findViewById<TextView>(R.id.card_title).text = title
        findViewById<TextView>(R.id.card_meta).text = meta
        findViewById<TextView>(R.id.card_bar).text = bar
        findViewById<View>(R.id.card).visibility = View.VISIBLE
    }

    private fun body(text: String) {
        findViewById<TextView>(R.id.body).text = text
    }

    private fun pills(list: List<String>) {
        if (list.isEmpty()) return
        findViewById<TextView>(R.id.pills).apply { text = list.joinToString(" · "); visibility = View.VISIBLE }
    }

    private fun primary(text: String, enabled: Boolean = true) {
        primary.text = text
        primary.isEnabled = enabled
        primary.visibility = View.VISIBLE
    }

    private fun label(text: String) {
        val v = TextView(this, null, 0, R.style.Ds_Label)
        v.text = text
        v.setPadding(0, 0, 0, dp(6))
        choices.addView(v)
    }

    /** GUTS, WITS and LUCK as three boxes (not buttons): the stat, its value large. */
    private fun statTiles() {
        val line = LinearLayout(this)
        for ((i, name) in listOf("GUTS", "WITS", "LUCK").withIndex()) {
            val box = LinearLayout(this)
            box.orientation = LinearLayout.VERTICAL
            box.gravity = Gravity.CENTER
            box.setBackgroundResource(R.drawable.ds_button)
            box.addView(TextView(this, null, 0, R.style.Ds_Label).apply { text = name })
            box.addView(TextView(this, null, 0, R.style.Ds_Mono).apply {
                text = engine.stat(name).toString()
                textSize = 20f
            })
            val lp = LinearLayout.LayoutParams(0, dp(56), 1f)
            if (i < 2) lp.rightMargin = dp(8)
            line.addView(box, lp)
        }
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(12)
        choices.addView(line, lp)
    }

    /** A full-width choice, 52 dp: label on the left, its stat (or price) on the right. */
    private fun row(text: String, tag: String, enabled: Boolean = true, selected: Boolean = false, onClick: () -> Unit) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setBackgroundResource(R.drawable.ds_button)
        row.setPadding(dp(12), 0, dp(12), 0)
        row.isEnabled = enabled
        row.isSelected = selected
        row.alpha = if (enabled) 1f else 0.45f
        row.setOnClickListener { if (enabled) onClick() }
        val label = TextView(this)
        label.text = text
        label.textSize = 15f
        label.setTypeface(label.typeface, android.graphics.Typeface.BOLD)
        label.setTextColor(getColorStateList(R.color.ds_on_button))
        label.isDuplicateParentStateEnabled = true
        label.maxLines = 2
        row.addView(label, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        if (tag.isNotEmpty()) {
            val t = TextView(this, null, 0, R.style.Ds_Mono)
            t.text = tag
            t.textSize = 12f
            t.setTextColor(getColorStateList(R.color.ds_on_button))
            t.isDuplicateParentStateEnabled = true
            row.addView(t)
        }
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, resources.getDimensionPixelSize(R.dimen.ds_choice))
        lp.bottomMargin = dp(10)
        choices.addView(row, lp)
    }

    private class Cell(val text: String, val sub: String?, val enabled: Boolean = true, val selected: Boolean = false, val textSp: Float? = null, val onClick: () -> Unit)

    /** Buttons in a grid ([columns] per row, [heightDp] tall): fight actions, the bag. */
    private fun grid(cells: List<Cell>, columns: Int, heightDp: Int) {
        cells.chunked(columns).forEach { chunk ->
            val line = LinearLayout(this)
            line.orientation = LinearLayout.HORIZONTAL
            for ((i, c) in chunk.withIndex()) {
                val b = TextView(this, null, 0, R.style.Ds_Button)
                b.text = if (c.sub == null) c.text else "${c.text}\n${c.sub}"
                b.textSize = c.textSp ?: if (c.sub == null) 12f else 15f
                b.maxLines = 3
                b.setPadding(dp(4), 0, dp(4), 0)
                b.isEnabled = c.enabled
                b.isSelected = c.selected
                b.alpha = if (c.enabled || c.text.isEmpty()) 1f else 0.45f
                b.setOnClickListener { if (c.enabled) c.onClick() }
                val lp = LinearLayout.LayoutParams(0, dp(heightDp), 1f)
                if (i < columns - 1) lp.rightMargin = dp(8)
                line.addView(b, lp)
            }
            repeat(columns - chunk.size) { line.addView(View(this), LinearLayout.LayoutParams(0, dp(heightDp), 1f)) }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp(8)
            choices.addView(line, lp)
        }
    }

    /** A bar of ten squares, filled in proportion: e-ink friendly, no colour. */
    private fun squares(value: Int, max: Int): String {
        val filled = if (max <= 0) 0 else (maxOf(0, value) * 10 + max - 1) / max
        return "■".repeat(filled.coerceIn(0, 10)) + "□".repeat(10 - filled.coerceIn(0, 10))
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = keys.handle(event) || super.dispatchKeyEvent(event)

    companion object {
        private const val SAVE = "stacks-save.json"
        private const val PER_PAGE = 3

        fun load(activity: Activity): StacksContent = StacksContent.parse(
            StacksContent.FILES.associateWith { name ->
                activity.assets.open("stacks/$name.json").bufferedReader().use { it.readText() }
            },
        )

        /** Floor and level of the saved run, for the Games tile; null when none was started. */
        fun summary(activity: Activity): Pair<Int, Int>? = runCatching {
            StacksState.fromJson(File(activity.filesDir, SAVE).readText())?.takeIf { it.screen != Screen.INTRO }?.let { it.floor to it.level }
        }.getOrNull()
    }
}
