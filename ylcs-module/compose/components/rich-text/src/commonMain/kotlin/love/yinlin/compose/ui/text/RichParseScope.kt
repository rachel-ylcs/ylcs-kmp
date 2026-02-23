package love.yinlin.compose.ui.text

import androidx.compose.runtime.Stable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import love.yinlin.extension.String
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Stable
class RichParseScope(
    private val converters: Map<String, RichConverter>
) {
    @OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
    @Suppress("RETURN_VALUE_NOT_USED")
    inline fun <reified T : JsonElement> JsonElement.cast(block: () -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
            (this@cast is T) holdsIn block
        }
        if (this is T) block()
    }

    fun parseIn(list: RichList, json: JsonElement) {
        val type = when (json) {
            is JsonNull -> RichType.Br.value
            is JsonPrimitive -> if (json.isString) RichType.Text.value else RichType.Emoji.value
            is JsonObject -> json[RichArg.Type.value].String
            else -> null
        } ?: return
        val converter = converters[type] ?: return
        with(converter) { convert(list, json) }
    }
}