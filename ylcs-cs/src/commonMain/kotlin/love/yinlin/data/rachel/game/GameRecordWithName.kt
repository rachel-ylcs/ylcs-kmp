package love.yinlin.data.rachel.game

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Stable
@Serializable
data class GameRecordWithName(
    val rid: Long, // [记录 ID]
    val gid: Int, // [游戏 ID]
    val ts: String, // [创建时间]
    val name: String, // [发布者昵称]
    val title: String, // [标题]
    val type: Game, // [游戏类型]
    val answer: JsonElement?, // [游戏答案]
    val result: JsonElement?, // [游戏结果]
)