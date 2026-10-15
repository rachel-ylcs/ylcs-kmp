package love.yinlin.compose.ui.widget

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import love.yinlin.extension.catchingNull
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.toJsonString

@Stable
open class DataTableJsonCell(initialValue: JsonElement = JsonNull) : DataTableCell() {
    private var value by mutableStateOf(initialValue)

    override val text: String get() = value.toJsonString()

    override fun update(value: String): Boolean {
        this.value = catchingNull { value.parseJsonValue<JsonElement>() } ?: return false
        return true
    }

    override fun captureValue(): () -> Unit {
        val initial = value
        return { value = initial }
    }
}