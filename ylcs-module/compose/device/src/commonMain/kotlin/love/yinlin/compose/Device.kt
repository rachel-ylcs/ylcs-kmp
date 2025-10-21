package love.yinlin.compose

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable

@Stable
@Serializable
class Device private constructor(
    val size: Size,
    val type: Type
) {
    @Stable
    @Serializable
    enum class Size {
        SMALL, MEDIUM, LARGE;

        fun select(small: Number, medium: Number, large: Number): Dp = when (this) {
            SMALL -> small
            MEDIUM -> medium
            LARGE -> large
        }.toDouble().dp

        fun select(small: TextStyle, medium: TextStyle, large: TextStyle): TextStyle = when (this) {
            SMALL -> small
            MEDIUM -> medium
            LARGE -> large
        }
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
            height >= width * 1.3f -> Type.PORTRAIT
            width >= height * 1.3f -> Type.LANDSCAPE
            else -> Type.SQUARE
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