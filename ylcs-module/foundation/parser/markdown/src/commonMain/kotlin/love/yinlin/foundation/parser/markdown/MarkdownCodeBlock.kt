package love.yinlin.foundation.parser.markdown

class MarkdownCodeBlock(code: String, info: String = "") : MarkdownBlock() {
    val info: String = info.trim()

    val content: String = code.replace("\r\n", "\n").replace('\r', '\n').let {
        if (it.isEmpty() || it.endsWith('\n')) it else "$it\n"
    }

    init {
        require('\n' !in info && '\r' !in info) { "Code fence info must be a single line" }
    }

    override fun equals(other: Any?): Boolean = other is MarkdownCodeBlock && content == other.content && info == other.info
    override fun hashCode(): Int = 31 * content.hashCode() + info.hashCode()
}
