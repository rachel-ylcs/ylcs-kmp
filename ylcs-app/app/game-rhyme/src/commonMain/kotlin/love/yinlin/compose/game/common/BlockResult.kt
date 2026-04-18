package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable

@Stable
enum class BlockResult(val title: String, val score: Int, val ratio: Float) {
    MISS(title = "MISS", score = 0, ratio = 1f),
    BAD(title = "BAD", score = 1, ratio = 0.8f),
    GOOD(title = "GOOD", score = 2, ratio = 0.5f),
    PERFECT(title = "PERFECT", score = 3, ratio = 0f);
}