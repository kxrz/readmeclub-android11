package club.readme.android.game.stacks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class StacksTest {

    private val content = StacksContent.parse(
        StacksContent.FILES.associateWith { File("src/main/assets/stacks/$it.json").readText() },
    )

    private fun engine(seed: Long = 42) = StacksEngine(content, StacksState().apply { this.seed = seed })

    @Test
    fun shippedContentFollowsTheRules() {
        assertEquals(emptyList<String>(), content.validate())
        assertTrue(content.rooms.size >= 60)
        assertTrue(content.rooms.count { it.kind == "author" } >= 12)
        assertTrue(content.enemies.count { it.named } >= 4)
        assertEquals(80, content.lines.size)
        assertEquals(30, content.achievements.size)
    }

    @Test
    fun classesSetStatsAndHp() {
        val e = engine()
        e.start("Spine Breaker")
        assertEquals(3, e.stat("GUTS"))
        assertEquals(10 + 2 * 3 + 2 * 1, e.maxHp)
        assertEquals(e.maxHp, e.state.hp)
        assertEquals(1, e.state.floor)
        assertTrue(e.state.rooms.size in 6..8)
    }

    @Test
    fun difficultyFollowsTheFloorAndCaps() {
        val e = engine()
        e.start("Speed Reader")
        assertEquals(4, e.difficulty)
        e.state.floor = 9
        assertEquals(7, e.difficulty)
        e.state.floor = 11 // past the two biomes: the first cycle, with a modifier
        assertEquals(7 + 1, e.difficulty)
        e.state.floor = 60
        assertEquals(9 + 3, e.difficulty) // capped at 9, plus at most 3 for the cycles
        assertNotNull(e.modifier)
    }

    @Test
    fun aRunResumesExactlyFromItsSave() {
        val a = engine(7)
        val b = engine(7)
        val actions = Random(1)
        repeat(300) {
            val step = actions.nextInt(1000)
            play(a, step)
            play(b, step)
        }
        assertEquals(a.state.toJson(), b.state.toJson())

        val restored = StacksEngine(content, StacksState.fromJson(a.state.toJson())!!)
        repeat(200) {
            val step = actions.nextInt(1000)
            play(a, step)
            play(restored, step)
        }
        assertEquals(a.state.toJson(), restored.state.toJson())
    }

    @Test
    fun aLongRunStaysConsistent() {
        for (seed in 1L..5L) {
            val e = engine(seed)
            val actions = Random(seed)
            repeat(4000) {
                play(e, actions.nextInt(1000))
                val s = e.state
                assertTrue("hp ${s.hp}", s.hp in 0..e.maxHp)
                assertTrue(s.bag.size <= StacksEngine.BAG)
                assertTrue(s.coins >= 0)
                assertTrue(s.stats.values.all { it >= 0 })
            }
            assertTrue("seed $seed reached floor ${e.state.count("floor")}", e.state.count("floor") >= 3)
            assertTrue(e.state.achievements.isNotEmpty())
        }
    }

    @Test
    fun deathKeepsTheHeroAndCostsHalfTheCoins() {
        val e = engine(3)
        e.start("Margin Scribbler")
        val actions = Random(3)
        while (e.state.count("deaths") == 0) {
            if (e.state.screen == Screen.FIGHT) e.state.hp = 1
            e.state.coins = 40
            play(e, actions.nextInt(1000))
        }
        assertEquals(Screen.DEATH, e.state.screen)
        assertEquals(20, e.state.coins)
        val level = e.state.level
        e.next()
        assertEquals(level, e.state.level)
        assertEquals(e.maxHp, e.state.hp)
        assertEquals(0, e.state.position)
    }

    @Test
    fun anUnknownSaveIsRefused() {
        assertEquals(null, StacksState.fromJson("{\"version\": 99}"))
        assertEquals(null, StacksState.fromJson("not json"))
    }

    /** One valid action for the current screen, chosen by [r]. */
    private fun play(e: StacksEngine, r: Int) {
        val s = e.state
        when (s.screen) {
            Screen.INTRO -> e.start(StacksEngine.CLASSES.keys.toList()[r % 3])
            Screen.ROOM -> {
                val enabled = e.choices().withIndex().filter { it.value.enabled }
                if (enabled.isNotEmpty()) e.choose(enabled[r % enabled.size].index)
            }
            Screen.OUTCOME, Screen.FLOOR_CLEARED, Screen.DEATH -> e.next()
            Screen.FIGHT -> {
                val usable = s.bag.indices.filter { e.carried().getOrNull(it)?.trinket == false }
                if (r % 7 == 0 && usable.isNotEmpty()) e.use(usable[r % usable.size])
                else e.fight(StacksEngine.Action.entries[r % 3])
            }
            Screen.LEVEL_UP -> e.levelUp(listOf("GUTS", "WITS", "LUCK")[r % 3])
            Screen.BAG_FULL -> e.resolveBag(r % 7 - 1)
        }
    }
}
