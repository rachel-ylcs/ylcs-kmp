package love.yinlin.compose.ui.layout

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.center
import love.yinlin.compose.ui.layout.ReorderingLayout.Info
import love.yinlin.compose.ui.layout.ReorderingLayout.Item
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Stable
internal object ReorderingHitTest {
    fun target(info: Info, dragging: Item, position: Offset, movement: Offset, keys: Set<Any>): Item? {
        if (movement == Offset.Zero) return null
        val center = position + dragging.size.center
        val originalCenter = info.position(dragging) + dragging.size.center
        var target: Item? = null
        var finalTarget = false
        var distance = Float.POSITIVE_INFINITY
        var progress = -1f
        for (item in info.items) {
            if (item.key == dragging.key || item.key !in keys || info.main(item.size) <= 0f) continue
            val itemCenter = info.position(item) + item.size.center
            val toward = itemCenter - originalCenter
            val advancing = if (info.isList) info.main(toward) * info.main(movement) > 0f
            else toward.x * movement.x + toward.y * movement.y > 0f
            if (!advancing) continue
            val fraction = crossing(info, position - movement, position, dragging.size, itemCenter)
            if (fraction < 0f) continue
            val inside = info.contains(position, dragging.size, itemCenter)
            val separation = if (info.isList) abs(info.main(itemCenter - center)) else (itemCenter - center).getDistanceSquared()
            val better = when {
                inside && !finalTarget -> true
                inside != finalTarget -> false
                inside -> separation < distance
                fraction != progress -> fraction > progress
                else -> separation < distance
            }
            if (better) {
                target = item
                finalTarget = inside
                distance = separation
                progress = fraction
            }
        }
        return target
    }

    private fun crossing(info: Info, from: Offset, to: Offset, size: Size, point: Offset): Float {
        var enter = 0f
        var exit = 1f
        val mainStart = info.main(from)
        val mainDelta = info.main(to) - mainStart
        val mainSize = info.main(size)
        val mainPoint = info.main(point)
        if (mainDelta == 0f) {
            if (mainPoint !in mainStart ..< mainStart + mainSize) return -1f
        }
        else {
            val first = (mainPoint - mainSize - mainStart) / mainDelta
            val last = (mainPoint - mainStart) / mainDelta
            enter = max(enter, min(first, last))
            exit = min(exit, max(first, last))
            if (enter >= exit) return -1f
        }
        if (!info.isList) {
            val crossStart = info.cross(from)
            val crossDelta = info.cross(to) - crossStart
            val crossSize = if (info.orientation == Orientation.Vertical) size.width else size.height
            val crossPoint = info.cross(point)
            if (crossDelta == 0f) {
                if (crossPoint !in crossStart ..< crossStart + crossSize) return -1f
            }
            else {
                val first = (crossPoint - crossSize - crossStart) / crossDelta
                val last = (crossPoint - crossStart) / crossDelta
                enter = max(enter, min(first, last))
                exit = min(exit, max(first, last))
                if (enter >= exit) return -1f
            }
        }
        return exit
    }

    fun edgeTarget(info: Info, dragging: Item, position: Offset, direction: Int, keys: Set<Any>): Item? {
        val center = position + dragging.size.center
        val physicalDirection = if (info.mainAxisReversed) -direction else direction
        var target: Item? = null
        var crossDistance = Float.POSITIVE_INFINITY
        var edge = Float.NEGATIVE_INFINITY
        for (item in info.items) {
            if (item.key == dragging.key || item.key !in keys || info.main(item.size) <= 0f || (item.index - dragging.index) * direction <= 0) continue
            val itemPosition = info.position(item)
            val main = info.main(itemPosition)
            if (main >= info.viewportSize || main + info.main(item.size) <= 0f) continue
            val itemCenter = itemPosition + item.size.center
            val distance = if (info.isList) 0f else abs(info.cross(itemCenter - center))
            val progress = info.main(itemCenter) * physicalDirection
            if (distance < crossDistance || (distance == crossDistance && progress > edge)) {
                target = item
                crossDistance = distance
                edge = progress
            }
        }
        return target
    }
}
