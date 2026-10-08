package club.readme.android.data

import club.readme.android.data.DeviceMatch.Candidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceMatchTest {

    private val registry = listOf(
        Candidate("small", "Small", "Acme", 528, 792, 3),
        Candidate("x4", "X4", "Xteink", 480, 800, 3),
        Candidate("x4pro", "X4 Pro", "Xteink", 480, 800, 3),
        Candidate("xteink-s4", "S4", "Xteink", 480, 800, 4),
        Candidate("tall", "Tall", "Acme", 824, 1648, 5),
        Candidate("unknown-screen", "Mystery", "Other", null, null, 9),
    )

    @Test fun modelNameWins() =
        assertEquals("xteink-s4", DeviceMatch.pick(registry, "XTEINK", "S4", 480, 800))

    @Test fun manufacturerModelSlug() {
        val unnamed = registry.map { if (it.slug == "xteink-s4") it.copy(name = "Pocket reader") else it }
        assertEquals("xteink-s4", DeviceMatch.pick(unnamed, "XTEINK", "S4", 480, 800))
    }

    @Test fun brandAndModelName() =
        assertEquals("tall", DeviceMatch.pick(registry, "Generic", "Acme Tall", 824, 1648))

    @Test fun exactResolutionPrefersMostFormats() =
        assertEquals("xteink-s4", DeviceMatch.pick(registry, "Acme", "Reader 6", 480, 800))

    @Test fun landscapeScreenCountsAsPortrait() =
        assertEquals("xteink-s4", DeviceMatch.pick(registry, "Acme", "Reader 6", 800, 480))

    @Test fun sameRatioWhenNoExactSize() =
        // 960 × 1600 is the 480 × 800 ratio, twice as large.
        assertEquals("xteink-s4", DeviceMatch.pick(registry, "Acme", "Reader 10", 960, 1600))

    @Test fun nothingFits() =
        assertNull(DeviceMatch.pick(registry, "Acme", "Tablet", 1200, 1600))
}
