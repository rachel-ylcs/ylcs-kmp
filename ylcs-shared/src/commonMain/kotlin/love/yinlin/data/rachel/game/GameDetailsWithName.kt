package love.yinlin.data.rachel.game

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Stable
@Serializable
data class GameDetailsWithName(
    val gid: Int, // [游戏 ID]
    val ts: String, // [创建时间]
    val title: String, // [标题]
    val type: Game, // [游戏类型]
    val reward: Int, // [游戏奖励]
    val num: Int, // [奖励名额]
    val cost: Int, // [参赛消耗]
    val winner: List<String>, // [游戏赢家]
    val info: JsonElement, // [游戏信息]
    val question: JsonElement, // [游戏问题]
    val answer: JsonElement, // [游戏答案]
    val isCompleted: Boolean, // [是否完成]
) {
    fun toPublic(name: String): GamePublicDetailsWithName = GamePublicDetailsWithName(
        gid = gid,
        name = name,
        ts = ts,
        title = title,
        type = type,
        reward = reward,
        num = num,
        cost = cost,
        winner = winner,
        info = info
    )
}