package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

@Stable
enum class BlockResult(
    val title: String,
    val score: Int,
    val color: Color,
    val alpha: Float,
) {
    MISS(title = "MISS", score = 0, color = Color(0xFFDC1B3F), alpha = 0f),
    BAD(title = "BAD", score = 1, color = Color(0xFF488CCE), alpha = 0.2f),
    GOOD(title = "GOOD", score = 2, color = Color(0xFF21B399), alpha = 0.35f),
    PERFECT(title = "PERFECT", score = 3, color = Color(0xFFC75A3A), alpha = 0.5f);
}