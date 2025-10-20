package love.yinlin.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

fun Offset.translate(x: Float = 0f, y: Float = 0f) = this.copy(x = this.x + x, y = this.y + y)

fun Offset.scale(dx: Float = 1f, dy: Float = 1f) = this.copy(x = this.x * dx, y = this.y * dy)

fun Offset.roundToIntOffset() = IntOffset(x = this.x.roundToInt(), y = this.y.roundToInt())

fun Offset.onLine(other: Offset, ratio: Float): Offset = Offset(x = this.x + (other.x - this.x) * ratio, y = this.y + (other.y - this.y) * ratio)

fun Offset.onCenter(other: Offset): Offset = Offset(x = (this.x + other.x) / 2, y = (this.y + other.y) / 2)

fun Offset.slope(other: Offset): Float = (other.x - this.x).let { if (it == 0f) Float.POSITIVE_INFINITY else (other.y - this.y) / it }

fun Offset.distance(other: Offset): Float = (this - other).getDistance()

fun Offset.Companion.cramerCenter(pos1: Offset, pos2: Offset, pos3: Offset, pos4: Offset): Offset {
    // Cramer Rule
    val a1 = pos1.y - pos3.y
    val b1 = pos3.x - pos1.x
    val c1 = pos1.x * pos3.y - pos3.x * pos1.y
    val a2 = pos2.y - pos4.y
    val b2 = pos4.x - pos2.x
    val c2 = pos2.x * pos4.y - pos4.x * pos2.y
    val d = a1 * b2 - a2 * b1
    return Offset((b1 * c2 - b2 * c1) / d, (a2 * c1 - a1 * c2) / d)
}

fun Size.translate(x: Float = 0f, y: Float = 0f) = this.copy(width = this.width + x, height = this.height + y)

fun Size.scale(dx: Float = 1f, dy: Float = 1f) = this.copy(width = this.width * dx, height = this.height * dy)