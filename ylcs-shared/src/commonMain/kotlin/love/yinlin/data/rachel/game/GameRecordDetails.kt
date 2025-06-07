package love.yinlin.data.rachel.game

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Stable
@Serializable
data class GameRecordDetails(
    val rid: Long, // [记录 ID]
    val gid: Int, // [游戏 ID]
    val uid: Int, // [用户 ID]
    val ts: String, // [创建时间]
    val answer: JsonElement, // [游戏答案]
    val result: JsonElement, // [游戏结果]
)