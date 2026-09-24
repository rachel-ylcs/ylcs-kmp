package love.yinlin.compose.ui.node

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import love.yinlin.compose.Colors

@Stable
fun Modifier.verticalFade(stops: List<Pair<Float, Float>>): Modifier = this.graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithCache {
    val brush = Brush.verticalGradient(
        colorStops = stops.map { [pos, alpha] ->
            pos to Colors.White.copy(alpha = alpha)
        }.toTypedArray()
    )
    onDrawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }
}

@Stable
fun Modifier.horizontalFade(stops: List<Pair<Float, Float>>): Modifier = this.graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithCache {
    val brush = Brush.horizontalGradient(
        colorStops = stops.map { [pos, alpha] ->
            pos to Colors.White.copy(alpha = alpha)
        }.toTypedArray()
    )
    onDrawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }
}