package love.yinlin.data.rachel.game

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CreateGameArgs(
    val title: String,
    val type: Game,
    val reward: Int,
    val num: Int,
    val cost: Int,
    val info: JsonElement,
    val question: JsonElement,
    val answer: JsonElement
)