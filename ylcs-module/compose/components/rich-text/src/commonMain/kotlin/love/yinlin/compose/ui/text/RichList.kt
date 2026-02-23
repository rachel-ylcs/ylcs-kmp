package love.yinlin.compose.ui.text

import androidx.compose.runtime.Stable
import kotlinx.serialization.json.JsonElement
import love.yinlin.extension.makeObject
import love.yinlin.extension.toJsonString

@Stable
abstract class RichList : RichValue() {
    @PublishedApi
    internal val items = mutableListOf<RichObject>()

    final override val json: JsonElement by lazy {
        makeObject {
            RichArg.Type.value with type
            arr(RichArg.Member.value) {
                for (item in items) add(item.json)
            }
            children()
        }
    }

    final override fun toString(): String = json.toJsonString()

    fun addNode(item: RichObject) { items += item }

    inline fun addListNode(item: RichList, content: RichList.() -> Unit) {
        item.content()
        items += item
    }
}