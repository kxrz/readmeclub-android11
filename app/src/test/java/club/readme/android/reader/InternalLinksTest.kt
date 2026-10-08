package club.readme.android.reader

import club.readme.android.reader.InternalLinks.GUIDES
import club.readme.android.reader.InternalLinks.NEWS
import club.readme.android.reader.InternalLinks.Target
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InternalLinksTest {

    @Test fun absoluteArticleLinks() {
        assertEquals(Target(NEWS, "x4-review"), InternalLinks.target("https://www.readme.club/news/x4-review"))
        assertEquals(Target(NEWS, "x4-review"), InternalLinks.target("https://readme.club/news/x4-review/"))
        assertEquals(Target(NEWS, "x4-review"), InternalLinks.target("https://www.readme.club/news/X4-Review?ref=a#top"))
    }

    @Test fun relativeArticleLink() = assertEquals(Target(NEWS, "x4-review"), InternalLinks.target("/news/x4-review"))

    @Test fun guideLinks() {
        assertEquals(Target(GUIDES, "how-to-flash"), InternalLinks.target("https://www.readme.club/guide/how-to-flash"))
        assertEquals(Target(GUIDES, "how-to-flash"), InternalLinks.target("/guide/how-to-flash#step-2"))
    }

    @Test fun externalLinksAreDropped() {
        assertNull(InternalLinks.target("https://silkscreenreader.com"))
        assertNull(InternalLinks.target("https://evil.test/news/x4-review"))
        assertNull(InternalLinks.target("https://www.readme.club.evil.test/news/x4-review"))
    }

    @Test fun otherSitePagesAreDropped() {
        assertNull(InternalLinks.target("https://www.readme.club/wallpapers"))
        assertNull(InternalLinks.target("https://www.readme.club/news/category/reviews"))
        assertNull(InternalLinks.target("https://www.readme.club/guide/brand/xteink"))
        assertNull(InternalLinks.target("https://www.readme.club/news/"))
    }
}
