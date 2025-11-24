package love.yinlin.compose.game

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.util.fastRoundToInt
import kotlin.jvm.JvmInline

@JvmInline
@Stable
value class AABB(private val value: Long) {
    val left: Float get() = (value shr 48).toShort().toFloat()
    val top: Float get() = (value shr 32).toShort().toFloat()
    val right: Float get() = (value shr 16).toShort().toFloat()
    val bottom: Float get() = value.toShort().toFloat()
    val size: Size get() = Size(right - left, bottom - top)
    val rect: Rect get() = Rect(left, top, right, bottom)

    constructor(left: Float, top: Float, right: Float, bottom: Float) : this(
        ((left.fastRoundToInt() and 0xFFFF).toLong() shl 48) or
                ((top.fastRoundToInt() and 0xFFFF).toLong() shl 32) or
                ((right.fastRoundToInt() and 0xFFFF).toLong() shl 16) or
                (bottom.fastRoundToInt() and 0xFFFF).toLong()
    )

    constructor(rect: Rect) : this(rect.left, rect.top, rect.right, rect.bottom)

    constructor(topLeft: Offset, size: Size) : this(Rect(topLeft, size))
}