package love.yinlin.data.rachel.game

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import love.yinlin.extension.toJson

@Stable
@Serializable
data class PreflightResult(
    val rid: Long,
    val info: JsonElement = Unit.toJson(),
    val question: JsonElement = Unit.toJson(),
    val answer: JsonElement = Unit.toJson(),
    val result: JsonElement = Unit.toJson()
)