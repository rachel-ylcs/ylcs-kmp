package love.yinlin.compose.ui.layout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.PinnableContainer
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import love.yinlin.compose.extension.mutableIntSizeStateOf
import love.yinlin.compose.extension.mutableOffsetStateOf

internal class ReorderingItemPosition(private val container: PinnableContainer?) {
    var coordinates: LayoutCoordinates? = null
    private var correction: Offset? = null
    private var placedPosition by mutableOffsetStateOf(Offset.Unspecified)
    private var placedSize by mutableIntSizeStateOf(IntSize.Zero)

    fun update(coordinates: LayoutCoordinates, info: ReorderingLayout.Info, key: Any) {
        this.coordinates = coordinates
        placedPosition = coordinates.positionInParent() - (correction ?: run {
            val item = info.item(key) ?: return
            val newCorrection = coordinates.positionInParent() - info.position(item)
            correction = newCorrection
            newCorrection
        })
        placedSize = coordinates.size
    }

    fun position(): Offset? = if (coordinates?.isAttached == true && placedPosition != Offset.Unspecified) placedPosition else null

    fun item(info: ReorderingLayout.Info, key: Any, index: Int): ReorderingLayout.Item? = position()?.let { info.placedItem(index, key, it, placedSize.toSize()) }

    fun pin(): PinnableContainer.PinnedHandle? = container?.pin()
}
