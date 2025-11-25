package love.yinlin.compose.game.traits

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import love.yinlin.compose.game.Pointer

interface Trigger : PreTransform {
    fun onEvent(pointer: Pointer): Boolean

    fun handle(pointer: Pointer): Boolean {
        val matrix = Matrix()
        for (transform in preTransform) {
            when (transform) {
                is Transform.Translate -> matrix.translate(transform.x, transform.y)
                is Transform.Scale -> {
                    val pivot = transform.pivot ?: Offset.Zero
                    if (pivot != Offset.Zero) {
                        matrix.translate(pivot.x, pivot.y)
                        matrix.scale(transform.x, transform.y)
                        matrix.translate(-pivot.x, -pivot.y)
                    }
                    else matrix.scale(transform.x, transform.y)
                }
                is Transform.Rotate -> {
                    val pivot = transform.pivot ?: Offset.Zero
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
        return onEvent(pointer.copy(position = matrix.map(pointer.position)))
    }
}