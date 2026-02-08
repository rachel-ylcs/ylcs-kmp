package love.yinlin.compose.ui.container

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.localComposition
import love.yinlin.compose.ui.node.condition
import kotlin.math.ln

private val LocalSurfaceTonalLevel = localComposition { 0 }

@Composable
@NonRestartableComposable
fun Surface(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
    contentAlignment: Alignment = Alignment.Center,
    shape: Shape = Theme.shape.rectangle,
    shadowElevation: Dp = 0.dp,
    tonalLevel: Int = 0,
    border: BorderStroke? = null,
    content: @Composable () -> Unit,
) {
    val absoluteTonalLevel = LocalSurfaceTonalLevel.current + tonalLevel
    CompositionLocalProvider(
        LocalColor provides Theme.color.onSurface,
        LocalColorVariant provides Theme.color.onSurfaceVariant,
        LocalSurfaceTonalLevel provides absoluteTonalLevel,
    ) {
        val shadowPx = with(LocalDensity.current) { shadowElevation.toPx() }
        val surfaceColor = Theme.color.surface
        val backgroundColor = if (absoluteTonalLevel <= 0) surfaceColor else {
            val alpha = ((4.5f * ln(absoluteTonalLevel / 2f + 1) + 2) / 100).coerceIn(0f, 1f)
            Theme.color.primary.copy(alpha = alpha).compositeOver(surfaceColor)
        }
        Box(
            modifier = modifier
                .condition(shadowPx > 0f) { graphicsLayer(shadowElevation = shadowPx, shape = shape) }
                .condition(border != null) { border(border, shape) }
                .clip(shape)
                .background(color = backgroundColor)
                .padding(contentPadding),
            contentAlignment = contentAlignment
        ) {
            content()
        }
    }
}