package love.yinlin.compose.game.data

import androidx.compose.runtime.Stable
import love.yinlin.data.rachel.rhyme.CharacterInfo

@Stable
data class RhymePlayConfig(
    val difficulty: RhymeDifficulty, // 难度
    val audioDelay: Long, // 音频延迟
    val character: CharacterInfo, // 角色
) {
    companion object {
        val Default = RhymePlayConfig(
            difficulty = RhymeDifficulty.Easy,
            audioDelay = 0L,
            character = CharacterInfo.Default
        )

        const val MIN_AUDIO_DELAY = -500L
        const val MAX_AUDIO_DELAY = 500L
    }
}