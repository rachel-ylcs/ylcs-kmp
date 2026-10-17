package love.yinlin.compose.ui.layout

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

internal interface ReorderingLayout {
    val info: Info

    fun prepareMove(from: Int, to: Int) { }

    suspend fun stopScroll() { }

    suspend fun scrollBy(value: Float): Float = 0f

    @Stable
    data class Item(val index: Int, val key: Any, val offset: Offset, val size: Size)

    @Stable
    class Info(
        val items: List<Item>,
        val orientation: Orientation,
        val viewportSize: Float,
        val beforeContentPadding: Int,
        val totalItemsCount: Int,
        val firstVisibleIndex: Int = 0,
        val mainAxisReversed: Boolean = false,
        val crossAxisReversed: Boolean = false,
        val canScrollBackward: Boolean = false,
        val canScrollForward: Boolean = false,
        val isList: Boolean = false,
        val crossAxisSize: Float = 0f,
    ) {
        internal constructor(orientation: Orientation) : this([], orientation, 0f, 0, 0, isList = true)

        fun item(key: Any): Item? = items.firstOrNull { it.key == key }

        fun main(offset: Offset): Float = if (orientation == Orientation.Vertical) offset.y else offset.x

        fun cross(offset: Offset): Float = if (orientation == Orientation.Vertical) offset.x else offset.y

        fun main(size: Size): Float = if (orientation == Orientation.Vertical) size.height else size.width

        fun axes(main: Float, cross: Float = 0f): Offset =
            if (orientation == Orientation.Vertical) Offset(cross, main) else Offset(main, cross)

        fun position(item: Item): Offset {
            val main = if (mainAxisReversed) viewportSize - beforeContentPadding - main(item.offset) - main(item.size)
            else main(item.offset) + beforeContentPadding
            val crossSize = if (orientation == Orientation.Vertical) item.size.width else item.size.height
            val cross = if (crossAxisReversed) crossAxisSize - cross(item.offset) - crossSize else cross(item.offset)
            return axes(main, cross)
        }

        fun placedItem(index: Int, key: Any, position: Offset, size: Size): Item {
            val main = if (mainAxisReversed) viewportSize - beforeContentPadding - main(position) - main(size)
            else main(position) - beforeContentPadding
            val crossSize = if (orientation == Orientation.Vertical) size.width else size.height
            val cross = if (crossAxisReversed) crossAxisSize - cross(position) - crossSize else cross(position)
            return Item(index, key, axes(main, cross), size)
        }

        fun contains(offset: Offset, size: Size, point: Offset): Boolean =
            if (isList) main(point) in main(offset) ..< main(offset) + main(size)
            else point.x in offset.x ..< offset.x + size.width && point.y in offset.y ..< offset.y + size.height
    }
}
