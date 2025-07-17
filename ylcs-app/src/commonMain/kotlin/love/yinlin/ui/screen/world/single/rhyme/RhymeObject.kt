package love.yinlin.ui.screen.world.single.rhyme

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import love.yinlin.data.music.RhymeLyricsConfig

// 游戏渲染
@Stable
internal data class RhymeDrawScope(
    val scope: DrawScope,
    val scale: Float
) {
    fun drawRect(color: Color, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill) = scope.drawRect(color, position * scale, size * scale, alpha, style)
    fun drawRect(brush: Brush, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill) = scope.drawRect(brush, position * scale, size * scale, alpha, style)
    fun drawCircle(color: Color, radius: Float, center: Offset, alpha: Float = 1f, style: DrawStyle = Fill) = scope.drawCircle(color, radius * scale, center * scale, alpha, style)
    fun drawCircle(brush: Brush, radius: Float, center: Offset, alpha: Float = 1f, style: DrawStyle = Fill) = scope.drawCircle(brush, radius * scale, center * scale, alpha, style)
}

// 游戏渲染实体
@Stable
internal sealed interface RhymeObject {
    fun RhymeDrawScope.draw()
}



// 游戏舞台
@Stable
internal class RhymeStage : RhymeObject {
    private var config: RhymeLyricsConfig? = null

    private val scene = mutableStateListOf<RhymeObject>()
    private val notes = mutableStateListOf<RhymeObject>()
    private val particles = mutableStateListOf<RhymeObject>()

    private var frame: Int = 0

    override fun RhymeDrawScope.draw() {

    }

    fun init(config: RhymeLyricsConfig) {
        this.config = config
    }

    fun clear() {
        config = null
        frame = 0
        scene.clear()
        notes.clear()
        particles.clear()
    }

    fun update(position: Long) {
        ++frame
    }
}