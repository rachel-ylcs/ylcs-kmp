package love.yinlin.compose.game.data

import androidx.compose.runtime.Stable

@Stable
data class RhymePlayResult(
    val score: Int,
    val statistics: List<Int>
)