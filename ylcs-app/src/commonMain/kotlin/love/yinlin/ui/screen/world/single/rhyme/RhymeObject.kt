package love.yinlin.ui.screen.world.single.rhyme

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import love.yinlin.data.music.RhymeLyricsConfig

// 手势操作事件
@Stable
internal enum class PointerEventType {
    Down, Move, Up
}

@Stable
internal data class PointerEvent(
    val type: PointerEventType,
    val startFrame: Int,
    val position: Offset
)

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

// 容器
@Stable
internal sealed interface RhymeContainer : RhymeObject {
    fun clear()
}

// 场景
@Stable
internal class RhymeScene : RhymeContainer {
    override fun RhymeDrawScope.draw() {

    }

    override fun clear() {

    }
}

// 音符管理器
@Stable
internal class RhymeNotesManager : RhymeContainer {
    override fun RhymeDrawScope.draw() {

    }

    override fun clear() {

    }
}

// 粒子管理
@Stable
internal class RhymeParticlesManager : RhymeContainer {
    override fun RhymeDrawScope.draw() {

    }

    override fun clear() {

    }
}

// 游戏舞台
@Stable
internal class RhymeStage {
    private var frame: Int = 0

    private var config: RhymeLyricsConfig? = null

    private val scene = RhymeScene()
    private var notes = RhymeNotesManager()
    private var particles = RhymeParticlesManager()

    fun init(lyrics: RhymeLyricsConfig) {
        config = lyrics
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
        // 处理手势事件
        
    }

    fun event(type: PointerEventType, position: Offset) {
        println(PointerEvent(type, frame, position))
    }

    fun RhymeDrawScope.draw() {

    }
}