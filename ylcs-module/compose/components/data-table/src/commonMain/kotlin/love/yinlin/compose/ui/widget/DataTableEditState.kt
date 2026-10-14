package love.yinlin.compose.ui.widget

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange

@Stable
class DataTableEditState internal constructor(
    private val table: DataTableState<*>,
    internal val cell: DataTableCellAddress,
    val type: DataTableCell,
) {
    private val initialValue = type.text
    internal val restoreValue = type.captureValue()
    internal val rawState = TextFieldState(initialValue, TextRange(0, initialValue.length))
    internal val isActive: Boolean get() = table.editing === this
    private var lastInput = initialValue
    val text: String get() = rawState.text.toString()

    var error by mutableStateOf(false)
        private set

    fun updateText(value: String): Boolean {
        if (!isActive || !type.acceptsInput(value)) return false
        rawState.setTextAndPlaceCursorAtEnd(value)
        onInput(value)
        return true
    }

    fun commit(): Boolean = table.finishEdit(this)

    fun cancel() = table.cancelEdit(this)

    internal fun onInput(value: String): Boolean {
        if (!isActive) return false
        if (lastInput != value) {
            lastInput = value
            error = !table.publishEdit(this, value)
        }
        return !error
    }
}
