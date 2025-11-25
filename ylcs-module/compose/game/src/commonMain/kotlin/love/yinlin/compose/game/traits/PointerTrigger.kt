package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import love.yinlin.compose.game.Pointer

@Stable
interface PointerTrigger : AABB, PreTransform {
    fun onPointerEvent(pointer: Pointer): Boolean

    fun internalHandlePointer(pointer: Pointer, block: (Pointer) -> Boolean): Boolean {
        val matrix = Matrix()
        for (transform in preTransform) {
            when (transform) {
                is Transform.Translate -> matrix.translate(transform.x, transform.y)
                is Transform.Scale -> {
                    val pivot = transform.pivot ?: center
                    if (pivot != Offset.Zero) {
                        matrix.translate(pivot.x, pivot.y)
                        matrix.scale(transform.x, transform.y)
                        matrix.translate(-pivot.x, -pivot.y)
                    }
                    else matrix.scale(transform.x, transform.y)
                }
                is Transform.Rotate -> {
                    val pivot = transform.pivot ?: center
                    if (pivot != Offset.Zero) {
                        matrix.translate(pivot.x, pivot.y)
                        matrix.rotateZ(transform.degrees)
                        matrix.translate(-pivot.x, -pivot.y)
                    }
                    else matrix.rotateZ(transform.degrees)
                }
            }
        }
        matrix.invert()
        val position = matrix.map(pointer.position)
        return if (position in this) block(pointer.copy(position = position)) else false
    }
}