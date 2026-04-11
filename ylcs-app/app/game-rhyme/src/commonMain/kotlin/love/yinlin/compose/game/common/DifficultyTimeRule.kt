package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable

@Stable
data class DifficultyTimeRule(
    val prepareTime: Int,
    val perfectTime: Int,
    val goodTime: Int,
    val badTime: Int
)