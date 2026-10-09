package club.readme.android.update

/**
 * Release notes are CHANGELOG.md sections (Keep a Changelog). The app ships the whole file
 * as an asset (the installed version's notes, readable offline), and the update manifest
 * carries the next version's section, so both can be read before and after updating.
 */
object ReleaseNotes {

    /** The body of "## [version] ..." up to the next version heading, or null if absent. */
    fun section(changelog: String, version: String): String? {
        val lines = changelog.lines()
        val start = lines.indexOfFirst { it.startsWith("## [$version]") }
        if (start < 0) return null
        val end = (start + 1 until lines.size).firstOrNull { lines[it].startsWith("## [") } ?: lines.size
        return lines.subList(start + 1, end).joinToString("\n").trim().ifEmpty { null }
    }

    /** Every version section, without the file's introduction. */
    fun allVersions(changelog: String): String {
        val lines = changelog.lines()
        val first = lines.indexOfFirst { it.startsWith("## [") }
        return if (first < 0) changelog else lines.drop(first).joinToString("\n")
    }

    /**
     * The small Markdown subset the changelog uses, as HTML for Html.fromHtml: "## " and "### "
     * headings, "- " bullets (with indented continuation lines), **bold** and `code`.
     */
    fun toHtml(markdown: String): String {
        val blocks = mutableListOf<String>()
        for (line in markdown.lines()) {
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> Unit
                line.startsWith("## ") -> blocks += "<p><big><b>${inline(trimmed.removePrefix("## ").replace("[", "").replace("]", ""))}</b></big></p>"
                line.startsWith("### ") -> blocks += "<p><b>${inline(trimmed.removePrefix("### "))}</b></p>"
                line.startsWith("- ") -> blocks += "<p>• ${inline(trimmed.removePrefix("- "))}</p>"
                // A wrapped bullet or paragraph line: join it to the block above.
                blocks.isNotEmpty() && line.startsWith(" ") ->
                    blocks[blocks.lastIndex] = blocks.last().removeSuffix("</p>") + " " + inline(trimmed) + "</p>"
                else -> blocks += "<p>${inline(trimmed)}</p>"
            }
        }
        return blocks.joinToString("")
    }

    private fun inline(text: String): String = text
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace(Regex("\\*\\*(.+?)\\*\\*"), "<b>$1</b>")
        .replace(Regex("`(.+?)`"), "$1")
}
