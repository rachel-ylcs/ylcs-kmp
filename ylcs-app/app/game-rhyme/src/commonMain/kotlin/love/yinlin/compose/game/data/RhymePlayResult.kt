package love.yinlin.compose.game.data

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Serializable
@Stable
data class RhymePlayResult(
    val score: Int,
    val statistics: List<Int>
)