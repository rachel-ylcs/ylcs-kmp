package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable

@Stable
data class BlockLine(
    val index: Int,
    val firstRawIndex: Int,
    val lastRawIndex: Int,
    val startDirection: BlockDirection,
    val endDirection: BlockDirection?,
    val text: String,
    val lineStart: Long,
    val lineEnd: Long
)