package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable

@Stable
data class DifficultyTimeRule(
    val prepareTime: Long,
    val perfectTime: Long,
    val goodTime: Long,
    val badTime: Long,
    val missTime: Long
)