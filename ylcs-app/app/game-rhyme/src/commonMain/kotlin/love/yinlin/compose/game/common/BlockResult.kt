package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import love.yinlin.compose.Colors

@Stable
enum class BlockResult(
    val title: String,
    val score: Int,
    val ratio: Float,
    val colors: List<Color>,
) {
    MISS(title = "MISS", score = 0, ratio = 1f, colors = listOf(Colors(0xFFFF0844), Colors(0xFFFFB199))),
    BAD(title = "BAD", score = 1, ratio = 0.8f, colors = listOf(Colors(0xFF6BBBFF), Colors(0xFFB8DCFF))),
    GOOD(title = "GOOD", score = 2, ratio = 0.5f, colors = listOf(Colors(0xFF43E97B), Colors(0xFF38F9D7))),
    PERFECT(title = "PERFECT", score = 3, ratio = 0f, colors = listOf(Colors(0xFFF6D365), Colors(0xFFFDA085)));
}