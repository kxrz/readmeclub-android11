package club.readme.android.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseNotesTest {

    private val changelog = """
        # Changelog

        Intro text.

        ## [1.0.6] - Unreleased

        ### Added
        - **Lights out**, a puzzle.

        ## [1.0.5] - 2026-10-09

        ### Changed
        - One bar
          everywhere.
    """.trimIndent()

    @Test fun findsASection() = assertEquals("### Added\n- **Lights out**, a puzzle.", ReleaseNotes.section(changelog, "1.0.6"))

    @Test fun lastSectionRunsToTheEnd() =
        assertEquals("### Changed\n- One bar\n  everywhere.", ReleaseNotes.section(changelog, "1.0.5"))

    @Test fun missingSection() = assertNull(ReleaseNotes.section(changelog, "0.9"))

    @Test fun allVersionsDropsTheIntro() = assertTrue(ReleaseNotes.allVersions(changelog).startsWith("## [1.0.6]"))

    @Test fun htmlForBulletsAndBold() = assertEquals(
        "<p><b>Added</b></p><p>• <b>Lights out</b>, a puzzle.</p>",
        ReleaseNotes.toHtml("### Added\n- **Lights out**, a puzzle."),
    )

    @Test fun wrappedBulletIsJoined() = assertEquals(
        "<p>• One bar everywhere.</p>",
        ReleaseNotes.toHtml("- One bar\n  everywhere."),
    )

    @Test fun versionHeadingCodeAndEscaping() = assertEquals(
        "<p><big><b>1.0.5 - 2026-10-09</b></big></p><p>a &lt; b, x</p>",
        ReleaseNotes.toHtml("## [1.0.5] - 2026-10-09\na < b, `x`"),
    )
}
