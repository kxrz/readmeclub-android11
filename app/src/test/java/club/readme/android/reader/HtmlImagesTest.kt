package club.readme.android.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class HtmlImagesTest {

    @Test fun findsSourcesAndUnescapesAmpersands() {
        val html = """<p>Hi</p><img alt="a" src="/api/media/file/a.jpg?w=1&amp;h=2"><IMG SRC='https://x.test/b.png' />"""
        assertEquals(
            listOf("/api/media/file/a.jpg?w=1&h=2", "https://x.test/b.png"),
            HtmlImages.sources(html),
        )
    }

    @Test fun ignoresDuplicatesAndOtherAttributes() {
        val html = """<img data-src="no.jpg" src="a.jpg"><img src="a.jpg">"""
        assertEquals(listOf("a.jpg"), HtmlImages.sources(html))
    }

    @Test fun resolvesAgainstTheCms() {
        assertEquals("https://write.readme.club/api/media/file/a.jpg", HtmlImages.resolve("/api/media/file/a.jpg"))
        assertEquals("https://cdn.test/a.jpg", HtmlImages.resolve("//cdn.test/a.jpg"))
        assertEquals("https://blob.test/a.jpg", HtmlImages.resolve("https://blob.test/a.jpg"))
    }
}
