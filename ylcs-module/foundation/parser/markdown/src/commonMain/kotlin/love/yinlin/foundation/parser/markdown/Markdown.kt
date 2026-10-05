package love.yinlin.foundation.parser.markdown

import org.intellij.lang.annotations.Language

class Markdown(val configuration: MarkdownConfiguration = MarkdownConfiguration()) {
    fun parse(@Language("MARKDOWN") source: String): MarkdownDocument = MarkdownParser(source, configuration).parse()
    fun encodeToString(element: MarkdownElement): String = MarkdownWriter(configuration).write(element)

    companion object {
        val Default: Markdown = Markdown()

        fun parse(@Language("MARKDOWN") source: String): MarkdownDocument = Default.parse(source)
        fun encodeToString(element: MarkdownElement): String = Default.encodeToString(element)
    }
}