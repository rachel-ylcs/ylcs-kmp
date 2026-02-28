package love.yinlin.common.rhyme.data

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import love.yinlin.compose.Colors

// 操作结果
@Stable
enum class ActionResult(
    val score: Int,
    val range: Float,
    val title: String,
    val colors: List<Color>,
) {
    MISS(
        score = 0,
        range = 0.3f,
        title = "MISS",
        colors = listOf(Colors(0xFFFF0844), Colors(0xFFFFB199))
    ),
    BAD(
        score = 1,
        range = 0.12f,
        title = "BAD",
        colors = listOf(Colors(0xFF6BBBFF), Colors(0xFFB8DCFF))
    ),
    GOOD(
        score = 2,
        range = 0.09f,
        title = "GOOD",
        colors = listOf(Colors(0xFF43E97B), Colors(0xFF38F9D7))
    ),
    PERFECT(
        score = 3,
        range = 0.06f,
        title = "PERFECT",
        colors = listOf(Colors(0xFFF6D365), Colors(0xFFFDA085))
    );

    fun startRange(center: Float) = center - range / 2
    fun endRange(center: Float) = center + range / 2
    fun inRange(center: Float, value: Float) = value >= startRange(center) && value <= endRange(center)

    companion object {
        const val COMBO_REWARD_COUNT = 30

        fun inRange(center: Float, value: Float): ActionResult? = when {
            PERFECT.inRange(center, value) -> PERFECT
            GOOD.inRange(center, value) -> GOOD
            BAD.inRange(center, value) -> BAD
            MISS.inRange(center, value) -> MISS
            else -> null
        }
    }
}