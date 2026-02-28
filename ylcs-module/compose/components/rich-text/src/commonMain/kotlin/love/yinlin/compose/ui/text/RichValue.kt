package love.yinlin.compose.ui.text

import androidx.compose.runtime.Stable
import kotlinx.serialization.json.JsonElement
import love.yinlin.extension.JsonObjectScope
import love.yinlin.extension.makeObject

@Stable
abstract class RichValue : RichObject {
    protected abstract fun JsonObjectScope.children()

    override val json: JsonElement by lazy {
        makeObject {
            RichArg.Type.value with type
            children()
        }
    }
}