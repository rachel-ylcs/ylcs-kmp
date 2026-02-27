package love.yinlin.common.rhyme

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Serializable
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

        const val MIN_AUDIO_DELAY = -500L
        const val MAX_AUDIO_DELAY = 500L
    }
}