package club.readme.android.reader

import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.text.style.QuoteSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import club.readme.android.data.Article
import club.readme.android.data.ContentStore

/** Turns an article into styled text for the reader: image, label, title, meta line, then the body HTML. */
object ArticleText {

    fun build(
        article: Article,
        label: String,
        store: ContentStore,
        resources: Resources,
        maxImageWidth: Int,
        maxImageHeight: Int,
    ): CharSequence {
        val text = SpannableStringBuilder()
        article.heroImage?.let { src ->
            // A third of the page at most, so the text starts on the first page.
            val image = loadImage(store, resources, src, maxImageWidth, maxImageHeight / 3)
            if (image.bounds.width() > 0) text.append("\uFFFC", ImageSpan(image), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE).append("\n\n")
        }
        val labelStart = text.length
        text.append(label.uppercase(), StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(RelativeSizeSpan(0.7f), labelStart, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.append("\n")
        val titleStart = text.length
        text.append(article.title, StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(RelativeSizeSpan(1.4f), titleStart, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.append("\n")
        val metaStart = text.length
        text.append(article.meta)
        text.setSpan(RelativeSizeSpan(0.8f), metaStart, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.append("\n\n")

        val images = Html.ImageGetter { src -> loadImage(store, resources, src, maxImageWidth, maxImageHeight) }
        val body = SpannableStringBuilder(Html.fromHtml(article.html, Html.FROM_HTML_MODE_LEGACY, images, null))
        blackQuotes(body)
        keepInternalLinks(body)
        while (body.isNotEmpty() && body.last().isWhitespace()) body.delete(body.length - 1, body.length)
        return text.append(body)
    }

    /** Cached image scaled to fit the page; an invisible placeholder if it was never downloaded. */
    private fun loadImage(store: ContentStore, resources: Resources, src: String, maxWidth: Int, maxHeight: Int): Drawable {
        val bitmap = BitmapFactory.decodeFile(store.imageFile(src).path)
            ?: return ColorDrawable(Color.TRANSPARENT).apply { setBounds(0, 0, 0, 0) }
        // Fit the page, and never blow small images up more than 1.5×.
        val scale = minOf(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height, 1.5f)
        return BitmapDrawable(resources, bitmap).apply {
            setBounds(0, 0, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt())
        }
    }

    /** External links become plain text; links to other articles and guides stay (underlined, tappable). */
    private fun keepInternalLinks(text: SpannableStringBuilder) {
        for (span in text.getSpans(0, text.length, URLSpan::class.java)) {
            if (InternalLinks.target(span.url) == null) text.removeSpan(span)
        }
    }

    /** Html.fromHtml draws blockquote stripes in blue; e-ink gets black. */
    private fun blackQuotes(text: SpannableStringBuilder) {
        for (span in text.getSpans(0, text.length, QuoteSpan::class.java)) {
            val start = text.getSpanStart(span)
            val end = text.getSpanEnd(span)
            text.removeSpan(span)
            text.setSpan(QuoteSpan(Color.BLACK), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
