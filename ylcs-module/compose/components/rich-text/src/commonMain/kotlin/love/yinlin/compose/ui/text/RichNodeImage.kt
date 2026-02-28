package love.yinlin.compose.ui.text

import androidx.compose.runtime.Stable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import love.yinlin.extension.FloatNull
import love.yinlin.extension.JsonObjectScope
import love.yinlin.extension.String

@RichExternal(RichDrawer::class)
@Stable
class RichNodeImage internal constructor(
    val uri: String,
    val width: Float,
    val height: Float
) : RichValue() {
    override val type: String = RichType.Image.value

    override fun JsonObjectScope.children() {
        RichArg.Uri.value with uri
        RichArg.Width.value with width
        RichArg.Height.value with height
    }

    @Stable
    data object Converter : RichConverter {
        override val type: String = RichType.Image.value

        override fun RichParseScope.convert(list: RichList, json: JsonElement) = json.cast<JsonObject> {
            list.image(
                uri = json[RichArg.Uri.value].String,
                width = json[RichArg.Width.value].FloatNull ?: 1f,
                height = json[RichArg.Height.value].FloatNull ?: 1f
            )
        }
    }
}

fun RichList.image(uri: String, width: Float = 1f, height: Float = 1f) = addNode(RichNodeImage(uri, width, height))