package love.yinlin.compose.ui.text

import androidx.compose.runtime.Stable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

@Stable
object RichNodeBr : RichObject {
    override val type: String = RichType.Br.value
    override val json: JsonElement = JsonNull

    @Stable
    data object Drawer : RichDrawer {
        override val type: String = RichType.Br.value

        override fun RichRenderScope.render(item: RichObject) {
            builder.appendLine()
        }
    }

    @Stable
    data object Converter : RichConverter {
        override val type: String = RichType.Br.value

        override fun RichParseScope.convert(list: RichList, json: JsonElement) {
            list.br()
        }
    }
}

fun RichList.br() = addNode(RichNodeBr)