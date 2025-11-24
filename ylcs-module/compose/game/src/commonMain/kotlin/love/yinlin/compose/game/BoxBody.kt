package love.yinlin.compose.game

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.util.fastRoundToInt
import love.yinlin.compose.game.traits.AABB
import kotlin.math.max

@Stable
open class BoxBody private constructor(private val value: Long) : AABB {
    override val left: Float get() = (value shr 48).toShort().toFloat()
    override val top: Float get() = (value shr 32).toShort().toFloat()
    override val right: Float get() = (value shr 16).toShort().toFloat()
    override val bottom: Float get() = value.toShort().toFloat()
    override val topLeft: Offset get() = Offset(left, top)
    override val size: Size get() = Size(right - left, bottom - top)
    override val rect: Rect get() = Rect(left, top, right, bottom)
    override val center: Offset get() = Offset((left + right) / 2f, (top + bottom) / 2f)
    override val radius: Float get() = max(right - left, bottom - top) / 2

    constructor(left: Float, top: Float, right: Float, bottom: Float) : this(
        ((left.fastRoundToInt() and 0xFFFF).toLong() shl 48) or
                ((top.fastRoundToInt() and 0xFFFF).toLong() shl 32) or
                ((right.fastRoundToInt() and 0xFFFF).toLong() shl 16) or
                (bottom.fastRoundToInt() and 0xFFFF).toLong()
    )

    constructor(rect: Rect) : this(rect.left, rect.top, rect.right, rect.bottom)

    constructor(topLeft: Offset, size: Size) : this(Rect(topLeft, size))

    override operator fun contains(point: Offset): Boolean = (point.x >= left) && (point.x < right) && (point.y >= top) && (point.y < bottom)
}