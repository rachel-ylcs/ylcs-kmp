package love.yinlin.compose

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.dp

@Stable
data class ModeShape(
    val small: Number,
    val medium: Number,
    val large: Number,
)

private val Number.shape: CornerBasedShape get() = RoundedCornerShape(this.toDouble().dp)

@Stable
data class ShapeSystem(
    val extraSmall: ModeShape,
    val small: ModeShape,
    val medium: ModeShape,
    val large: ModeShape,
    val extraLarge: ModeShape,
) {
    fun toShapes(size: Device.Size): Shapes = when (size) {
        Device.Size.SMALL -> Shapes(
            extraSmall = extraSmall.small.shape,
            small = small.small.shape,
            medium = medium.small.shape,
            large = large.small.shape,
            extraLarge = extraLarge.small.shape
        )
        Device.Size.MEDIUM -> Shapes(
            extraSmall = extraSmall.medium.shape,
            small = small.medium.shape,
            medium = medium.medium.shape,
            large = large.medium.shape,
            extraLarge = extraLarge.medium.shape
        )
        Device.Size.LARGE -> Shapes(
            extraSmall = extraSmall.large.shape,
            small = small.large.shape,
            medium = medium.large.shape,
            large = large.large.shape,
            extraLarge = extraLarge.large.shape
        )
    }
}

val DefaultShapeSystem = ShapeSystem(
    extraSmall = ModeShape(3, 3.5, 4),
    small = ModeShape(5, 5.5, 6),
    medium = ModeShape(7, 7.5, 8),
    large = ModeShape(9, 9.5, 10),
    extraLarge = ModeShape(11, 11.5, 12),
)