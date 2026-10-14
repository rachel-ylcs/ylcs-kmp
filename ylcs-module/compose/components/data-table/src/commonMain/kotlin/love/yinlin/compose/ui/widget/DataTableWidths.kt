package love.yinlin.compose.ui.widget

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Stable
internal class DataTableWidths(private val values: Map<String, Dp>) {
    val total: Dp = values.values.fold(0.dp) { sum, width -> sum + width }
    operator fun get(key: String): Dp = values.getValue(key)
}