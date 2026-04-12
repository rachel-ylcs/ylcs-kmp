package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import love.yinlin.compose.Colors
import love.yinlin.compose.game.data.RhymeDifficulty

@Stable
enum class BlockResult(
    val title: String,
    val score: Int,
    val ranges: Map<RhymeDifficulty, Float>,
    val colors: List<Color>,
) {
    MISS(
        title = "MISS",
        score = 0,
        ranges = emptyMap(),
        colors = listOf(Colors(0xFFFF0844), Colors(0xFFFFB199))
    ),
    BAD(
        title = "BAD",
        score = 1,
        ranges = mapOf(),
        colors = listOf(Colors(0xFF6BBBFF), Colors(0xFFB8DCFF))
    ),
    GOOD(
        title = "GOOD",
        score = 2,
        ranges = mapOf(
            RhymeDifficulty.Easy to 0.7f,
            RhymeDifficulty.Medium to 0.6f,
            RhymeDifficulty.Hard to 0.5f,
            RhymeDifficulty.Extreme to 0.4f
        ),
        colors = listOf(Colors(0xFF43E97B), Colors(0xFF38F9D7))
    ),
    PERFECT(
        title = "PERFECT",
        score = 3,
        ranges = mapOf(
            RhymeDifficulty.Easy to 0.4f,
            RhymeDifficulty.Medium to 0.3f,
            RhymeDifficulty.Hard to 0.2f,
            RhymeDifficulty.Extreme to 0.2f
        ),
        colors = listOf(Colors(0xFFF6D365), Colors(0xFFFDA085))
    );

    companion object {
        val PrepareDuration = mapOf(
            RhymeDifficulty.Easy to 2000,
            RhymeDifficulty.Medium to 1500,
            RhymeDifficulty.Hard to 1250,
            RhymeDifficulty.Extreme to 1000
        )
    }
}