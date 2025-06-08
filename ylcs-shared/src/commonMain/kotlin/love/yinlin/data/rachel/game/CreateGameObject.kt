package love.yinlin.data.rachel.game

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Stable
@Serializable
data class CreateGameObject(
    val token: String, // [用户 token]
    val title: String, // [标题]
    val type: Game, // [游戏类型]
    val reward: Int, // [游戏奖励]
    val num: Int, // [奖励名额]
    val cost: Int, // [参赛消耗]
    val info: JsonElement, // [游戏信息]
    val question: JsonElement, // [游戏问题]
    val answer: JsonElement, // [游戏答案]
)