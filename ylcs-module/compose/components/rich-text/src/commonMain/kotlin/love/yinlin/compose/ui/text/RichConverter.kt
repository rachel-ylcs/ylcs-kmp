package love.yinlin.compose.ui.text

import androidx.compose.runtime.Stable
import kotlinx.serialization.json.JsonElement

@Stable
interface RichConverter {
    val type: String

    fun RichParseScope.convert(list: RichList, json: JsonElement)
}