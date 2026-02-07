package love.yinlin.compose.ui.layout

import androidx.compose.runtime.Stable
import androidx.compose.ui.layout.MeasurePolicy

@Stable
object MeasurePolicies {
    @Stable
    val Empty = MeasurePolicy { _, constraints ->
        layout(constraints.minWidth, constraints.minHeight) {}
    }

    @Stable
    val Space = MeasurePolicy { _, constraints ->
        layout(if (constraints.hasFixedWidth) constraints.maxWidth else 0, if (constraints.hasFixedHeight) constraints.maxHeight else 0) {}
    }
}