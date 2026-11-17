package love.yinlin.compose.ui.layout

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Stable
sealed interface PagerPaginationCells {
    /** 固定显示 [count] 列。 */
    @Stable
    data class Fixed(val count: Int) : PagerPaginationCells {
        init {
            require(count > 0) { "count must be greater than 0." }
        }
    }

    /** 根据可用宽度自适应列数，每个单元的宽度不会小于 [minSize]（容器不足以容纳时除外）。 */
    @Stable
    data class Adaptive(val minSize: Dp) : PagerPaginationCells {
        init {
            require(minSize > 0.dp && minSize != Dp.Infinity) { "minSize must be a finite value greater than 0." }
        }
    }
}
