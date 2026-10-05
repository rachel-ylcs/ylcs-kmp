package love.yinlin.foundation.parser.markdown

import androidx.annotation.IntRange

data class MarkdownConfiguration(
    val tables: Boolean = true,
    val taskLists: Boolean = true,
    val strikethrough: Boolean = true,
    @IntRange(1, 256) val maxDepth: Int = 128,
    val maxNodes: Int = 1000000,
    val maxSourceLength: Int = 16 * 1024 * 1024,
) {
    init {
        require(maxDepth in 1 .. 256) { "maxDepth must be between 1 and 256" }
        require(maxNodes > 0) { "maxNodes must be positive" }
        require(maxSourceLength > 0) { "maxSourceLength must be positive" }
    }
}
