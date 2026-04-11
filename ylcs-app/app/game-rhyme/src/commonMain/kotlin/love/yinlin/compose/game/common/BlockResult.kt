package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import love.yinlin.compose.Colors
import love.yinlin.compose.game.data.RhymeDifficulty

@Stable
enum class BlockResult(
    val title: String,
    val score: Int,
    val durationRanges: Map<RhymeDifficulty, Int>,
    val colors: List<Color>,
) {
    MISS(
        title = "MISS",
        score = 0,
        durationRanges = emptyMap(),
        colors = listOf(Colors(0xFFFF0844), Colors(0xFFFFB199))
    ),
    BAD(
        title = "BAD",
        score = 1,
        durationRanges = mapOf(
            RhymeDifficulty.Easy to 500,
            RhymeDifficulty.Medium to 400,
            RhymeDifficulty.Hard to 300,
            RhymeDifficulty.Extreme to 200
        ),
        colors = listOf(Colors(0xFF6BBBFF), Colors(0xFFB8DCFF))
    ),
    GOOD(
        title = "GOOD",
        score = 2,
        durationRanges = mapOf(
            RhymeDifficulty.Easy to 375,
            RhymeDifficulty.Medium to 300,
            RhymeDifficulty.Hard to 225,
            RhymeDifficulty.Extreme to 150
        ),
        colors = listOf(Colors(0xFF43E97B), Colors(0xFF38F9D7))
    ),
    PERFECT(
        title = "PERFECT",
        score = 3,
        durationRanges = mapOf(
            RhymeDifficulty.Easy to 250,
            RhymeDifficulty.Medium to 200,
            RhymeDifficulty.Hard to 150,
            RhymeDifficulty.Extreme to 100
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