package love.yinlin.compose.ui.layout

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import love.yinlin.compose.extension.mutableRefStateOf

internal class ReorderingListLayout(orientation: Orientation, var sourceKeys: List<Any>) : ReorderingLayout {
    override var info by mutableRefStateOf(ReorderingLayout.Info(orientation))
    var expectedSourceKeys: List<Any>? = null
}
