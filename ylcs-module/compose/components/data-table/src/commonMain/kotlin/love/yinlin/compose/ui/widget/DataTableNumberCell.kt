package love.yinlin.compose.ui.widget

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType

@Stable
open class DataTableNumberCell(initialValue: Double = 0.0) : DataTableCell(), DataTableSortableCell {
    private companion object {
        val DataTableNumberInput = Regex("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]*)(?:[eE][+-]?[0-9]*)?|[+-]?")
    }

    init {
        require(initialValue.isFinite()) { "DataTable number must be finite" }
    }

    private var value by mutableDoubleStateOf(initialValue)

    override val text: String by derivedStateOf { value.toString().removeSuffix(".0") }
    override val keyboardType: KeyboardType = KeyboardType.Decimal
    override val sortType = DataTableNumberCell::class
    override fun acceptsInput(value: String): Boolean = DataTableNumberInput.matches(value)

    override fun update(value: String): Boolean {
        val input = value.trim()
        if (input.isEmpty() || !acceptsInput(input)) return false
        val number = input.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return false
        this.value = number
        return true
    }

    override fun compareTo(other: DataTableSortableCell): Int = value.compareTo((other as DataTableNumberCell).value)

    override fun captureValue(): () -> Unit {
        val initial = value
        return { value = initial }
    }
}
