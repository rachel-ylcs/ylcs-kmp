package love.yinlin.common

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import love.yinlin.extension.localComposition

@Stable
@Serializable
class Device private constructor(
    val size: Size,
    val type: Type
) {
    @Stable
    @Serializable
    enum class Size { SMALL, MEDIUM, LARGE; }
    @Stable
    @Serializable
    enum class Type { PORTRAIT, LANDSCAPE, SQUARE; }

    constructor(width: Dp) : this(
        size = when {
            width <= 420.dp -> SMALL
            width <= 900.dp -> MEDIUM
            else -> LARGE
        },
        type = when {
            width <= 420.dp -> PORTRAIT
            width <= 900.dp -> SQUARE
            else -> LANDSCAPE
        }
    )

    constructor(width: Dp, height: Dp) : this(
        size = when {
            width <= 420.dp -> SMALL
            width <= 900.dp -> MEDIUM
            else -> LARGE
        },
        type = when {
            height >= width * 1.3f -> PORTRAIT
            width >= height * 1.3f -> LANDSCAPE
            else -> SQUARE
        }
    )

    override fun toString(): String = "[size=$size, type=$type]"

    override fun equals(other: Any?): Boolean = when {
        other == null || other !is Device -> false
        other.size == this.size && other.type == this.type -> true
        else -> false
    }

    override fun hashCode(): Int = type.hashCode() + size.hashCode()
}

val LocalDevice = localComposition<Device>()