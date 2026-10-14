package love.yinlin.compose.ui.widget

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.reflect.KClass

@Stable
class DataTableColumn(
    val title: String,
    val cellType: KClass<out DataTableCell>? = null,
    val width: Dp = Dp.Unspecified,
) {
    init {
        require(title.isNotBlank()) { "DataTable column title must not be blank" }
        require(width == Dp.Unspecified || (width.value.isFinite() && width > 0.dp)) { "DataTable column width must be positive" }
    }
}
