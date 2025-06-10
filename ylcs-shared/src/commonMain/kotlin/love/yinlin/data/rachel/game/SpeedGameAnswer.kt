package love.yinlin.data.rachel.game

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

@Stable
@Serializable
data class SpeedGameAnswer(
    val start: Boolean = true,
    val rid: Long = 0,
    val ts: Long = 0L,
    val answer: JsonElement = JsonNull,
)