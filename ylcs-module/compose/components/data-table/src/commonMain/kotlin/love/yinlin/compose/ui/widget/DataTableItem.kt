package love.yinlin.compose.ui.widget

import androidx.compose.runtime.Stable
import love.yinlin.reflect.metaSimpleClassName

@Stable
abstract class DataTableItem {
    protected abstract val items: List<DataTableCell>

    @PublishedApi
    internal fun cellAt(index: Int): DataTableCell = items[index]

    internal fun cellOrNull(index: Int): DataTableCell? = items.getOrNull(index)

    inline operator fun <reified C : DataTableCell> get(index: Int): C = cellAt(index) as? C ?: throw ClassCastException("DataTable item cell $index is not ${metaSimpleClassName<C>()}")
}
