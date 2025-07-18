package love.yinlin.ui.screen.world.single.rhyme

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.roundToIntSize
import love.yinlin.common.Colors
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.extension.roundToIntOffset
import love.yinlin.extension.translate

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
    private val scope: DrawScope,
    private val scale: Float
) {
    fun drawRect(color: Color, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill) =
        scope.drawRect(color, position * scale, size * scale, alpha, style)
    fun drawRect(brush: Brush, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill) =
        scope.drawRect(brush, position * scale, size * scale, alpha, style)
    fun drawCircle(color: Color, radius: Float, center: Offset, alpha: Float = 1f, style: DrawStyle = Fill) =
        scope.drawCircle(color, radius * scale, center * scale, alpha, style)
    fun drawCircle(brush: Brush, radius: Float, center: Offset, alpha: Float = 1f, style: DrawStyle = Fill) =
        scope.drawCircle(brush, radius * scale, center * scale, alpha, style)
    fun drawArc(color: Color, startAngle: Float, sweepAngle: Float, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill) =
        scope.drawArc(color, startAngle, sweepAngle, false, position * scale, size * scale, alpha, style)
    fun drawArc(brush: Brush, startAngle: Float, sweepAngle: Float, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill) =
        scope.drawArc(brush, startAngle, sweepAngle, false, position * scale, size * scale, alpha, style)
    fun drawCircleImage(image: ImageBitmap, radius: Float, center: Offset) {
        scope.clipPath(Path().apply { addOval(Rect(center * scale, radius * scale)) }) {
            drawImage(
                image = image,
                dstOffset = (center.translate(-radius, -radius) * scale).roundToIntOffset(),
                dstSize = (Size(radius * 2, radius * 2) * scale).roundToIntSize(),
                filterQuality = FilterQuality.High
            )
        }
    }
}

// 游戏渲染实体
@Stable
private sealed interface RhymeObject {
    fun update(frame: Int, position: Long)
    fun RhymeDrawScope.draw()
}

// 进度板
@Stable
private class ProgressBoard(
    private val duration: Long,
    private val record: ImageBitmap
) : RhymeObject {
    // 游戏进度
    private var progress: Float by mutableFloatStateOf(0f)

    override fun update(frame: Int, position: Long) {
        progress = if (duration == 0L) 0f else (position / duration.toFloat()).coerceIn(0f, 1f)
    }

    override fun RhymeDrawScope.draw() {
        val center = Offset(960f, 540f)
        // 画封面
        drawCircleImage(
            image = record,
            radius = 60f,
            center = center
        )
        // 画时长
        drawArc(
            color = Colors.White,
            startAngle = -90f,
            sweepAngle = 360f,
            position = center.translate(-64f, -64f),
            size = Size(128f, 128f),
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
        // 画进度
        drawArc(
            color = Colors.Steel4,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            position = center.translate(-64f, -64f),
            size = Size(128f, 128f),
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
    }
}

// 轨道
@Stable
private class Track : RhymeObject {
    // 当前按下轨道
    private val current: Int? by mutableStateOf(null)

    override fun update(frame: Int, position: Long) {
        // 画射线
        
    }

    override fun RhymeDrawScope.draw() {

    }
}

// 歌词板
@Stable
private class LyricsBoard : RhymeObject {
    override fun update(frame: Int, position: Long) {

    }

    override fun RhymeDrawScope.draw() {

    }
}

// 分数板
@Stable
private class ScoreBoard : RhymeObject {
    override fun update(frame: Int, position: Long) {

    }

    override fun RhymeDrawScope.draw() {

    }
}

// 连击板
@Stable
private class ComboBoard : RhymeObject {
    override fun update(frame: Int, position: Long) {

    }

    override fun RhymeDrawScope.draw() {

    }
}

// 场景
@Stable
private class Scene(
    lyrics: RhymeLyricsConfig,
    record: ImageBitmap
) : RhymeObject {
    private val progressBoard = ProgressBoard(lyrics.duration, record)
    private val track = Track()
    private val lyricsBoard = LyricsBoard()
    private val scoreBoard = ScoreBoard()
    private val comboBoard = ComboBoard()

    override fun update(frame: Int, position: Long) {
        progressBoard.update(frame, position)
        track.update(frame, position)
        lyricsBoard.update(frame, position)
        scoreBoard.update(frame, position)
        comboBoard.update(frame, position)
    }

    override fun RhymeDrawScope.draw() {
        with(progressBoard) { draw() }
        with(track) { draw() }
        with(lyricsBoard) { draw() }
        with(scoreBoard) { draw() }
        with(comboBoard) { draw() }
    }
}

// 音符管理器
@Stable
private class NotesManager(
    private val lyrics: RhymeLyricsConfig,
    private val speed: Int
) : RhymeObject {
    override fun update(frame: Int, position: Long) {

    }

    override fun RhymeDrawScope.draw() {

    }
}

// 粒子管理
@Stable
private class ParticlesManager : RhymeObject {
    override fun update(frame: Int, position: Long) {

    }

    override fun RhymeDrawScope.draw() {

    }
}

// 游戏舞台
@Stable
internal class RhymeStage {
    private var frame: Int = 0
    private var scene: Scene? = null
    private var notes: NotesManager? = null
    private var particles: ParticlesManager? = null

    fun onInitialize(lyrics: RhymeLyricsConfig, record: ImageBitmap, speed: Int) {
        scene = Scene(lyrics, record)
        notes = NotesManager(lyrics, speed)
        particles = ParticlesManager()
    }

    fun onClear() {
        frame = 0
        scene = null
        notes = null
        particles = null
    }

    fun onUpdate(position: Long) {
        ++frame
        scene?.update(frame, position)
        notes?.update(frame, position)
        particles?.update(frame, position)
    }

    fun onPointerEvent(type: PointerEventType, position: Offset) {
        println(PointerEvent(type, frame, position))
    }

    fun RhymeDrawScope.onDraw() {
        scene?.apply { draw() }
        notes?.apply { draw() }
        particles?.apply { draw() }
    }

    fun onResult(): RhymeResult {
        return RhymeResult(0)
    }
}