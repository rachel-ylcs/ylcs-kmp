package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable

@Stable
data class BlockUpdateResult(
    val result: BlockResult,
    val combo: Int,
    val id: Long
)