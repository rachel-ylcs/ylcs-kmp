package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable

@Stable
data class DifficultyResultRule(
    val prepareTime: Int,
    val perfectRatio: Float,
    val goodRatio: Float
)