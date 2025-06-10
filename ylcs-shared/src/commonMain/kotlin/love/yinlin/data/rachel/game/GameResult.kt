package love.yinlin.data.rachel.game

import androidx.compose.runtime.Stable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Stable
@Serializable
sealed interface GameResult {
    @Stable
    @Serializable
    @SerialName("Settlement")
    data class Settlement(
        val isCompleted: Boolean, // [是否完成]
        val reward: Int, // [奖励]
        val rank: Int, // [名次]
        val info: JsonElement, // [其他信息]
    ) : GameResult

    @Stable
    @Serializable
    @SerialName("Time")
    data class Time(
        val rid: Long, // [记录ID]
        val ts: Long, // [开始时间]
    ) : GameResult
}