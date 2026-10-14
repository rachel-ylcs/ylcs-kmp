package love.yinlin.compose.ui.widget

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
open class DataTableTextCell(initialValue: String = "") : DataTableCell(), DataTableSortableCell {
    private var value by mutableStateOf(initialValue)

    override val text: String get() = value
    override val sortType = DataTableTextCell::class

    override fun update(value: String): Boolean {
        this.value = value
        return true
    }

    override fun compareTo(other: DataTableSortableCell): Int = value.compareTo((other as DataTableTextCell).value, ignoreCase = true)

    override fun captureValue(): () -> Unit {
        val initial = value
        return { value = initial }
    }
}
