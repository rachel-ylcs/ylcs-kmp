package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable

@Stable
data class BlockLine(
    val index: Int,
    val corner: BlockCorner?,
    val text: String,
    val lineStart: Long,
    val lineEnd: Long
)