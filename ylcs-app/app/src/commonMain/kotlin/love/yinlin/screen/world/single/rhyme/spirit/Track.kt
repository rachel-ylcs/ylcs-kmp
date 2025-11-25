package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import love.yinlin.compose.Colors
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.traits.Spirit
import love.yinlin.compose.game.traits.BoxBody
import love.yinlin.compose.game.traits.Transform
import love.yinlin.compose.game.traits.Visible
import love.yinlin.screen.world.single.rhyme.RhymeManager

@Stable
class Track(
    val left: Offset,
    val right: Offset
) {
    companion object {
        // 顶点
        val Vertices = Offset(960f, 0f)
    }
}

@Stable
class TrackUI(
    rhymeManager: RhymeManager,
) : Spirit(rhymeManager), BoxBody, Visible {
    override val preTransform: List<Transform> = listOf(Transform.Translate(0f, -216f))
    override val size: Size = Size(1920f, 1296f)

    override fun Drawer.onDraw() {
        line(Colors.Red4, Track.Vertices, bottomLeft, Stroke(2f))
        line(Colors.Red4, Track.Vertices, bottomRight, Stroke(2f))
    }
}