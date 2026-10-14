package love.yinlin.compose.ui.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.text.SimpleEllipsisText

@Stable
open class DataTableBoolCell(initialValue: Boolean = false) : DataTableCell() {
    private var value by mutableStateOf(initialValue)

    override val text: String get() = if (value) "true" else "false"
    override val editable: Boolean = false

    override fun update(value: String): Boolean {
        val input = value.trim()
        this.value = when {
            input.equals("true", ignoreCase = true) -> true
            input.equals("false", ignoreCase = true) -> false
            else -> return false
        }
        return true
    }

    override fun captureValue(): () -> Unit {
        val initial = value
        return { value = initial }
    }

    override fun toggle(): Boolean {
        value = !value
        return true
    }

    @Composable
    override fun Display(modifier: Modifier) {
        val checked = value
        Row(
            modifier = modifier.padding(horizontal = Theme.padding.h),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.e),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon = if (checked) Icons.CheckBox else Icons.CheckBoxOutlineBlank,
                color = if (checked) Theme.color.primary else Theme.color.onSurfaceVariant,
                modifier = Modifier.size(Theme.size.smallIcon)
            )
            SimpleEllipsisText(text = text, style = Theme.typography.v8)
        }
    }
}
