package love.yinlin.compose.ui.floating

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.window.PopupPositionProvider

@Stable
internal class FlyoutPositionProvider(
    private val position: FlyoutPosition,
    private val space: Int
) : PopupPositionProvider {
    private fun leftPosition(anchorBounds: IntRect, popupContentSize: IntSize, windowSize: IntSize): IntOffset {
        var x = anchorBounds.left - (popupContentSize.width + space)
        if (x < 0) {
            val xCorrection = (anchorBounds.right + space + popupContentSize.width - windowSize.width).fastCoerceAtLeast(0)
            x = anchorBounds.right + space - xCorrection
        }
        val y = (anchorBounds.top + anchorBounds.bottom - popupContentSize.height) / 2
        return IntOffset(x, y)
    }

    fun topPosition(anchorBounds: IntRect, popupContentSize: IntSize, windowSize: IntSize): IntOffset {
        var x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        if (x < 0) {
            val xCorrection = (anchorBounds.left + popupContentSize.width - windowSize.width).fastCoerceAtLeast(0)
            x = anchorBounds.left - xCorrection
        } else if (x + popupContentSize.width > windowSize.width) {
            x = (anchorBounds.right - popupContentSize.width).fastCoerceAtLeast(0)
        }
        var y = anchorBounds.top - popupContentSize.height - space
        if (y < 0) y = anchorBounds.bottom + space
        return IntOffset(x, y)
    }

    fun rightPosition(anchorBounds: IntRect, popupContentSize: IntSize, windowSize: IntSize): IntOffset {
        var x = anchorBounds.right + space
        if (x + popupContentSize.width > windowSize.width) {
            x = (anchorBounds.left - (popupContentSize.width + space)).fastCoerceAtLeast(0)
        }
        val y = (anchorBounds.top + anchorBounds.bottom - popupContentSize.height) / 2
        return IntOffset(x, y)
    }

    fun bottomPosition(anchorBounds: IntRect, popupContentSize: IntSize, windowSize: IntSize): IntOffset {
        var x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        if (x < 0) {
            val xCorrection = (anchorBounds.left + popupContentSize.width - windowSize.width).fastCoerceAtLeast(0)
            x = anchorBounds.left - xCorrection
        } else if (x + popupContentSize.width > windowSize.width) {
            x = (anchorBounds.right - popupContentSize.width).fastCoerceAtLeast(0)
        }
        var y = anchorBounds.bottom + space
        if (y + popupContentSize.height > windowSize.height) y = anchorBounds.top - popupContentSize.height - space
        return IntOffset(x, y)
    }

    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        return when (position) {
            FlyoutPosition.Left -> leftPosition(anchorBounds, popupContentSize, windowSize)
            FlyoutPosition.Top -> topPosition(anchorBounds, popupContentSize, windowSize)
            FlyoutPosition.Right -> rightPosition(anchorBounds, popupContentSize, windowSize)
            FlyoutPosition.Bottom -> bottomPosition(anchorBounds, popupContentSize, windowSize)
        }
    }
}
