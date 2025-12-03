package love.yinlin.screen.world.single.rhyme

import androidx.compose.runtime.Stable

@Stable
data class RhymePlayConfig(
    val difficulty: RhymeDifficulty, // 难度
    val audioDelay: Long, // 消除延迟
) {
    companion object {
        val Default = RhymePlayConfig(
            difficulty = RhymeDifficulty.Easy,
            audioDelay = 0L
        )
    }
}