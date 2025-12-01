package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset

@Stable
sealed interface Transform {
    @Stable
    data class Translate(val x: Float, val y: Float) : Transform {
        constructor(offset: Offset) : this(offset.x, offset.y)
    }

    @Stable
    data class Scale(val x: Float, val y: Float = x, val pivot: Offset? = null) : Transform

    @Stable
    data class Rotate(val degrees: Float, val pivot: Offset? = null) : Transform

    @Stable
    data class Matrix(val matrix: androidx.compose.ui.graphics.Matrix): Transform
}