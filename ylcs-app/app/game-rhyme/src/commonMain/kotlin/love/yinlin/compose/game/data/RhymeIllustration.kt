package love.yinlin.compose.game.data

import androidx.compose.runtime.Stable
import love.yinlin.data.rachel.rhyme.CharacterInfo

@Stable
data class RhymeIllustration(
    val info: CharacterInfo,
    val url: String,
    val unlocked: Boolean
)