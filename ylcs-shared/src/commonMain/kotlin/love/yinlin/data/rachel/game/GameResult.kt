package love.yinlin.data.rachel.game

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Stable
@Serializable
data class GameResult(
    val isCompleted: Boolean, // [是否完成]
    val reward: Int, // [奖励]
    val rank: Int, // [名次]
    val info: JsonElement, // [其他信息]
)