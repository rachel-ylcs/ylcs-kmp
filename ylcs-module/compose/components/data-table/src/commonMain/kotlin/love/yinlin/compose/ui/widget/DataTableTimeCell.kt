package love.yinlin.compose.ui.widget

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.LocalTime
import love.yinlin.extension.DateEx

@Stable
open class DataTableTimeCell(initialValue: LocalTime = DateEx.Current.time) : DataTableCell(), DataTableSortableCell {
    private var value by mutableStateOf(initialValue)

    override val text: String get() = DateEx.Formatter.standardTime.format(value) ?: ""
    override val sortType = DataTableTimeCell::class

    override fun update(value: String): Boolean {
        this.value = DateEx.Formatter.standardTime.parse(value.trim()) ?: return false
        return true
    }

    override fun compareTo(other: DataTableSortableCell): Int = value.compareTo((other as DataTableTimeCell).value)

    override fun captureValue(): () -> Unit {
        val initial = value
        return { value = initial }
    }
}