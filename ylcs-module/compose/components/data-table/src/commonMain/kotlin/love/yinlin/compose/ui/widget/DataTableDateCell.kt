package love.yinlin.compose.ui.widget

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.LocalDate
import love.yinlin.extension.DateEx

@Stable
open class DataTableDateCell(initialValue: LocalDate = DateEx.Current.date) : DataTableCell(), DataTableSortableCell {
    private var value by mutableStateOf(initialValue)

    override val text: String get() = DateEx.Formatter.standardDate.format(value) ?: ""
    override val sortType = DataTableDateCell::class

    override fun update(value: String): Boolean {
        this.value = DateEx.Formatter.standardDate.parse(value.trim()) ?: return false
        return true
    }

    override fun compareTo(other: DataTableSortableCell): Int = value.compareTo((other as DataTableDateCell).value)

    override fun captureValue(): () -> Unit {
        val initial = value
        return { value = initial }
    }
}