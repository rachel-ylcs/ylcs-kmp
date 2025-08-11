package love.yinlin.ui.screen.world.single.rhyme

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import love.yinlin.common.Colors

// MISS 环境
@Stable
internal class MissEnvironment : RhymeDynamic(), RhymeContainer.Rectangle {
    @Stable
    private data class CornerData(
        val radius: Float,
        val brush: Brush
    )
    @Stable
    private data class Corner(
        val position: Offset,
        val data: List<CornerData>
    )

    companion object {
        const val FPA = RhymeConfig.FPS * 2
    }

    override val position: Offset = Offset.Zero
    override val size: Size = Size.Game

    private val fullRadius = size.minDimension * 0.75f
    private val corners = arrayOf(Track.Tracks[0], Track.Tracks[2], Track.Tracks[5], Track.Tracks[7]).map { position ->
        Corner(
            position = position,
            data = (0 .. FPA).map {
                val x = it / FPA.toFloat()
                val progress = (0.3235f * x * x * x - 1.071f * x * x + 1.7475f * x).coerceIn(0f, 1f)
                val radius = fullRadius * progress
                CornerData(
                    radius = radius,
                    brush = Brush.radialGradient(
                        colors = listOf(Colors.Red5.copy(alpha = progress * 0.75f), Colors.Transparent),
                        center = position,
                        radius = radius
                    )
                )
            }
        )
    }
    var stateFrame: Int by mutableIntStateOf(0)

    override fun onUpdate(position: Long) {
        if (stateFrame > 0) --stateFrame
    }

    override fun DrawScope.onDraw(textManager: RhymeTextManager) {
        for (corner in corners) {
            corner.data.getOrNull(stateFrame)?.let { (radius, brush) ->
                circle(
                    brush = brush,
                    position = corner.position,
                    radius = radius
                )
            }
        }
    }
}