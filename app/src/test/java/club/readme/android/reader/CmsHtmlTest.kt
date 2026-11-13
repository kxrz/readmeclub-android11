package club.readme.android.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CmsHtmlTest {

    /** As the CMS renders an image in an article body (2026-11, live). */
    private val figure = """<figure class="cms-image"><img src="https://blob.test/case.webp" """ +
        """srcset="https://blob.test/case-400x223.webp 400w, https://blob.test/case-900x503.webp 900w, https://blob.test/case.webp 1024w" """ +
        """sizes="(min-width: 768px) 720px, 100vw" alt="Case" width="1024" height="572" loading="lazy" decoding="async" /></figure>"""

    @Test fun imageTakesTheSmallestSizeThatFillsTheCacheWidth() {
        val html = CmsHtml.forReader(figure, 480)
        assertEquals(listOf("https://blob.test/case-900x503.webp"), HtmlImages.sources(html))
        assertFalse(html.contains("srcset"))
        assertFalse(html.contains("sizes="))
        assertTrue(html.contains("""alt="Case""""))
    }

    @Test fun imageNarrowerThanTheScreenTakesTheWidestSize() {
        val html = CmsHtml.forReader("""<img src="o.webp" srcset="a.webp 200w, b.webp 300w">""", 480)
        assertEquals(listOf("b.webp"), HtmlImages.sources(html))
    }

    @Test fun exactWidthAndAmpersandsKeptAsTheCacheExpects() {
        val html = CmsHtml.forReader("""<img src="o.jpg" srcset="a.jpg?w=480&amp;q=1 480w, b.jpg 900w">""", 480)
        assertEquals(listOf("a.jpg?w=480&q=1"), HtmlImages.sources(html))
    }

    @Test fun imageWithoutSrcsetIsLeftAsItIs() {
        val img = """<p>Hi</p><img src="/api/media/file/a.jpg">"""
        assertEquals(img, CmsHtml.forReader(img, 480))
    }

    @Test fun figureBecomesABlockAndItsCaptionASmallLineUnderTheImage() {
        val html = CmsHtml.forReader(figure.replace("</figure>", "<figcaption>The &amp; case</figcaption></figure>"), 480)
        assertFalse(html.contains("figure"))
        assertTrue(html.startsWith("<div><img "))
        assertTrue(html.endsWith("/><p><small><i>The &amp; case</i></small></p></div>"))
    }

    @Test fun linkedImageKeepsItsLink() {
        val html = CmsHtml.forReader(figure.replace("<img", """<a class="image-link" href="/news/a-story"><img""").replace("</figure>", "</a></figure>"), 480)
        assertTrue(html.contains("""<a class="image-link" href="/news/a-story"><img src="https://blob.test/case-900x503.webp""""))
    }

    @Test fun youTubeEmbedBecomesAVideoLine() {
        val html = CmsHtml.forReader(
            """<p>Before</p><figure class="embed"><iframe src="https://www.youtube.com/embed/hkDNszfnIJY" loading="lazy" allowfullscreen></iframe></figure><p>After</p>""",
            480,
        )
        assertEquals(
            "<p>Before</p><p><b>Video</b> (YouTube). <i>To watch it: Menu › Open on your phone.</i></p><p>After</p>",
            html,
        )
    }

    @Test fun verticalTikTokEmbedWithCaption() {
        val html = CmsHtml.forReader(
            """<figure class="embed embed--vertical" style="max-width:340px;margin-inline:auto"><iframe src="https://www.tiktok.com/player/v1/7699081418936208672?rel=0" style="display:block"></iframe><figcaption>Unboxing the S4</figcaption></figure>""",
            480,
        )
        assertEquals("<p><b>Video</b> (TikTok): Unboxing the S4. <i>To watch it: Menu › Open on your phone.</i></p>", html)
    }

    @Test fun captionEndingWithAStopGetsNoSecondOne() {
        val html = CmsHtml.forReader("""<figure class="embed"><iframe src="https://player.vimeo.com/video/1"></iframe><figcaption>Watch this!</figcaption></figure>""", 480)
        assertEquals("<p><b>Video</b> (Vimeo): Watch this! <i>To watch it: Menu › Open on your phone.</i></p>", html)
    }

    @Test fun bareIframeFromAnUnknownHost() {
        val html = CmsHtml.forReader("""<p>A</p><iframe src="https://maps.test/x"></iframe>""", 480)
        assertEquals("<p>A</p><p><b>Video</b>. <i>To watch it: Menu › Open on your phone.</i></p>", html)
    }

    @Test fun textWithoutCmsMarkupIsUnchanged() {
        val html = """<h2>Steps</h2><p>Open <a href="/guide/x">the guide</a>, then <b>tap</b>.</p><ul><li>One</li></ul>"""
        assertEquals(html, CmsHtml.forReader(html, 480))
    }
}
