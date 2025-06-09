package love.yinlin.data.rachel.game

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import love.yinlin.extension.toJson

@Stable
@Serializable
data class PreflightResult(
    val ok: Boolean, // [预检结果]
    val msg: String, // [预检文本]
    val info: JsonElement, // [预检信息]
) {
    constructor() : this(ok = true, msg = "", info = Unit.toJson())
    constructor(msg: String) : this(ok = false, msg = msg, info = Unit.toJson())
}