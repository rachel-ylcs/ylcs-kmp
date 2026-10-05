package love.yinlin.foundation.parser.markdown

data class MarkdownCode(val content: String) : MarkdownInline() {
    init {
        require('\n' !in content && '\r' !in content) { "Inline code must be a single line" }
    }
}