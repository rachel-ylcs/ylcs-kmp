package love.yinlin.compose.ui.text

import androidx.compose.runtime.Stable
import kotlinx.serialization.json.JsonElement

@Stable
interface RichObject {
    val type: String
    val json: JsonElement
}