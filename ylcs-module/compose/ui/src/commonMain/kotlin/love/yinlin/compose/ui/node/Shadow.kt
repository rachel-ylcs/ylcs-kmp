package love.yinlin.compose.ui.node

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import love.yinlin.compose.Colors

fun Modifier.shadow(shape: Shape, radius: Dp): Modifier = this.dropShadow(
    shape = shape,
    shadow = Shadow(
        radius = radius,
        color = Colors.Dark.copy(alpha = 0.75f),
        offset = DpOffset(radius, radius)
    )
)