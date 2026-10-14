package love.yinlin.compose.ui.widget

import androidx.compose.runtime.Stable
import kotlin.reflect.KClass

/**
 * 单元格的可选排序能力，实现此接口后，所在列自动显示排序按钮。
 * [compareTo] 应直接比较单元格保存的实际值。
 * [sortType] 相同的实例必须能互相比较；派生类可沿用基类的排序组，其他组及不可排序单元格分组处理。
 */
@Stable
interface DataTableSortableCell : Comparable<DataTableSortableCell> {
    val sortType: KClass<out DataTableSortableCell> get() = this::class
}
