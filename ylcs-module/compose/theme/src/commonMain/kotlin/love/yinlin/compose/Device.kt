package love.yinlin.compose

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import love.yinlin.compose.extension.staticLocalComposition

@Stable
@Serializable
data class Device(val size: Size, val type: Type) {
    @Stable
    @Serializable
    enum class Size {
        SMALL, MEDIUM, LARGE;
    }

    @Stable
    @Serializable
    enum class Type {
        PORTRAIT, LANDSCAPE, SQUARE;
    }

    constructor(width: Dp) : this(
        size = when {
            width <= 420.dp -> Size.SMALL
            width <= 900.dp -> Size.MEDIUM
            else -> Size.LARGE
        },
        type = when {
            width <= 420.dp -> Type.PORTRAIT
            width <= 900.dp -> Type.SQUARE
            else -> Type.LANDSCAPE
        }
    )

    constructor(width: Dp, height: Dp) : this(
        size = when {
            width <= 420.dp -> Size.SMALL
            width <= 900.dp -> Size.MEDIUM
            else -> Size.LARGE
        },
        type = when {
            height >= width * 1.5f -> Type.PORTRAIT
            width >= height * 1.3333f -> Type.LANDSCAPE
            else -> Type.SQUARE
        }
    )
}

val LocalDevice = staticLocalComposition<Device>()