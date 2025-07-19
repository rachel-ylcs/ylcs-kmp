package love.yinlin.ui.screen.world.single.rhyme

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.util.fastJoinToString
import androidx.compose.ui.util.fastMapIndexed
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

private operator fun DrawStyle.times(scale: Float): DrawStyle = when (this) {
    is Fill -> Fill
    is Stroke -> Stroke(
        width = this.width * scale,
        miter = this.miter * scale,
        cap = this.cap,
        join = this.join,
        pathEffect = this.pathEffect
    )
}

private operator fun Shadow.times(scale: Float): Shadow = this.copy(
    offset = this.offset * scale,
    blurRadius = this.blurRadius * scale
)

// 游戏渲染
@Stable
internal data class RhymeDrawScope(
    private val scope: DrawScope,
    private val scale: Float,
    private val textData: RhymeTextData
) {
    @Stable
    internal data class RhymeTextData(
        val font: Font,
        val measurer: TextMeasurer
    )

    @Stable
    internal inner class RhymeMeasureResult(val result: TextLayoutResult) {
        val width: Float get() = result.size.width.toFloat() / scale
        val height: Float get() = result.size.height.toFloat() / scale

        fun clipWidth(w: Float) = RhymeMeasureResult(result.copy(size = IntSize((w * scale).toInt(), result.size.height)))
        fun clipHeight(h: Float) = RhymeMeasureResult(result.copy(size = IntSize(result.size.width, (h * scale).toInt())))
    }

    // 路径
    @Stable
    internal inner class RhymePath {
        internal val path = Path()
        fun moveTo(position: Offset): RhymePath {
            path.moveTo(position.x * scale, position.y * scale)
            return this
        }
        fun lineTo(position: Offset): RhymePath {
            path.lineTo(position.x * scale, position.y * scale)
            return this
        }
        fun addOval(position: Offset, size: Size): RhymePath {
            path.addOval(Rect(position * scale, size * scale))
            return this
        }
        fun addRect(position: Offset, size: Size): RhymePath {
            path.addRect(Rect(position * scale, size * scale))
            return this
        }
    }

    fun drawLine(color: Color, start: Offset, end: Offset, style: Stroke, alpha: Float = 1f) =
        scope.drawLine(color, start * scale, end * scale, style.width * scale, style.cap, style.pathEffect, alpha)
    fun drawLine(brush: Brush, start: Offset, end: Offset, style: Stroke, alpha: Float = 1f) =
        scope.drawLine(brush, start * scale, end * scale, style.width * scale, style.cap, style.pathEffect, alpha)
    fun drawRect(color: Color, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill) =
        scope.drawRect(color, position * scale, size * scale, alpha, style * scale)
    fun drawRect(brush: Brush, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill) =
        scope.drawRect(brush, position * scale, size * scale, alpha, style * scale)
    fun drawCircle(color: Color, radius: Float, center: Offset, alpha: Float = 1f, style: DrawStyle = Fill) =
        scope.drawCircle(color, radius * scale, center * scale, alpha, style * scale)
    fun drawCircle(brush: Brush, radius: Float, center: Offset, alpha: Float = 1f, style: DrawStyle = Fill) =
        scope.drawCircle(brush, radius * scale, center * scale, alpha, style * scale)
    fun drawArc(color: Color, startAngle: Float, sweepAngle: Float, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill) =
        scope.drawArc(color, startAngle, sweepAngle, false, position * scale, size * scale, alpha, style * scale)
    fun drawArc(brush: Brush, startAngle: Float, sweepAngle: Float, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill) =
        scope.drawArc(brush, startAngle, sweepAngle, false, position * scale, size * scale, alpha, style * scale)
    fun drawPath(color: Color, path: RhymePath, alpha: Float = 1f, style: DrawStyle = Fill) =
        scope.drawPath(path.path, color, alpha, style * scale)
    fun drawPath(brush: Brush, path: RhymePath, alpha: Float = 1f, style: DrawStyle = Fill) =
        scope.drawPath(path.path, brush, alpha, style * scale)
    fun measureText(text: String, expectHeight: Float): RhymeMeasureResult {
        val virtualHeight = expectHeight * scale
        val size = textData.measurer.measure(
            text = text,
            style = TextStyle(
                fontSize = TextUnit(virtualHeight, TextUnitType.Sp),
                fontFamily = FontFamily(textData.font)
            ),
            maxLines = 1
        ).size
        val ratio = virtualHeight / size.height
        val actualTextSize = virtualHeight * ratio
        return RhymeMeasureResult(textData.measurer.measure(
            text = text,
            style = TextStyle(
                fontSize = TextUnit(actualTextSize, TextUnitType.Sp),
                fontFamily = FontFamily(textData.font)
            ),
            maxLines = 1
        ))
    }
    fun drawText(
        result: RhymeMeasureResult,
        position: Offset,
        color: Color,
        alpha: Float = 1f,
        shadow: Shadow? = null,
        textDecoration: TextDecoration? = null,
        drawStyle: DrawStyle? = null
    ) = scope.drawText(
        textLayoutResult = result.result,
        color = color,
        topLeft = position * scale,
        alpha = alpha,
        shadow = shadow?.times(scale),
        textDecoration = textDecoration,
        drawStyle = drawStyle?.times(scale)
    )
    fun drawImage(image: ImageBitmap, position: Offset, size: Size) {
        scope.drawImage(
            image = image,
            dstOffset = (position * scale).roundToIntOffset(),
            dstSize = (size * scale).roundToIntSize(),
            filterQuality = FilterQuality.High
        )
    }

    inline fun clipRect(position: Offset, size: Size, block: RhymeDrawScope.() -> Unit) =
        scope.clipRect(position.x * scale, position.y * scale, (position.x + size.width) * scale, (position.y + size.height) * scale) { block() }

    inline fun clipPath(path: RhymePath, block: RhymeDrawScope.() -> Unit) =
        scope.clipPath(path.path) { block() }
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
        val center = Offset(960f, 360f)
        val arcWidth = 8f
        val rectTopLeft = center.translate(-64f, -64f)
        val rectSize = Size(128f, 128f)
        // 画封面
        clipPath(RhymePath().addOval(rectTopLeft, rectSize)) {
            drawImage(
                image = record,
                position = rectTopLeft,
                size = rectSize
            )
        }
        // 画时长
        drawArc(
            color = Colors.White,
            startAngle = -90f,
            sweepAngle = 360f,
            position = rectTopLeft,
            size = rectSize,
            style = Stroke(width = arcWidth, cap = StrokeCap.Round)
        )
        // 画进度
        drawArc(
            color = Colors.Green4,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            position = rectTopLeft,
            size = rectSize,
            style = Stroke(width = arcWidth, cap = StrokeCap.Round)
        )
    }
}

// 轨道
@Stable
private class Track : RhymeObject {
    // 当前按下轨道
    private val current: Int? by mutableStateOf(null)

    private val trackWidth = 20f
    private val tracks = listOf(
        Offset(0f, 0f),
        Offset(0f, 540f),
        Offset(0f, 1080f),
        Offset(640f, 1080f),
        Offset(1280f, 1080f),
        Offset(1920f, 1080f),
        Offset(1920f, 540f),
        Offset(1920f, 0f),
    ).map { Offset(960f, 360f) to it }

    private fun RhymeDrawScope.drawTrackLine(start: Offset, end: Offset, width: Float) {
        // 阴影
        repeat(5) {
            drawLine(
                color = Colors.Steel3,
                start = start,
                end = end,
                style = Stroke(width = width * (1.15f + it * 0.15f), cap = StrokeCap.Round),
                alpha = 0.15f - (it * 0.03f)
            )
        }
        // 光带
        drawLine(
            color = Colors.Steel4,
            start = start,
            end = end,
            style = Stroke(width = width, cap = StrokeCap.Round),
            alpha = 0.7f
        )
        // 高光
        drawLine(
            color = Colors.White,
            start = start,
            end = end,
            style = Stroke(width = width * 0.8f, cap = StrokeCap.Round),
            alpha = 0.8f
        )
    }

    override fun update(frame: Int, position: Long) {

    }

    override fun RhymeDrawScope.draw() {
        // 画轨道射线
        for ((start, end) in tracks) {
            drawTrackLine(
                start = start,
                end = end,
                width = trackWidth
            )
        }
    }
}

// 歌词板
@Stable
private class LyricsBoard(private val lyrics: RhymeLyricsConfig) : RhymeObject {
    private var currentIndex = -1
    private var text by mutableStateOf("")
    private var frameTable by mutableStateOf(emptyList<Int>())
    private var startFrame = 0
    private var progress by mutableFloatStateOf(0f)

    override fun update(frame: Int, position: Long) {
        val lines = lyrics.lyrics
        val nextLine = lines.getOrNull(currentIndex + 1) ?: return
        if (position >= nextLine.start) {
            ++currentIndex
            val theme = nextLine.theme
            // 合并字符显示 (因为 plain text 内可能包含不是 Action 的空白字符)
            text = theme.fastJoinToString("") { it.ch }
            // 计算每一个字符需要的帧数
            frameTable = theme.fastMapIndexed { i, action ->
                (action.end - (theme.getOrNull(i - 1)?.end ?: 0)) * RhymeConfig.FPS / 1000
            }
            // 修正此行的起始帧
            startFrame = frame - (position - nextLine.start).toInt() * RhymeConfig.FPS / 1000
        }
        lines.getOrNull(currentIndex)?.theme?.let { theme ->
            var currentLength = 0f // 当前字符长
            val totalLength = text.length // 总字符长
            var totalFrame = frame - startFrame // 距离行首帧数
            if (totalLength != theme.size || totalLength != frameTable.size) return@let
            for (i in theme.indices) {
                val action = theme[i]
                val costFrame = frameTable[i] // 此字符消耗帧数
                if (totalFrame <= costFrame) { // 余数
                    currentLength += action.ch.length * totalFrame / costFrame.toFloat() // 根据帧数等比例计算字符长度
                    break
                }
                else { // 消耗
                    currentLength += action.ch.length
                    totalFrame -= costFrame
                }
            }
            progress = currentLength / totalLength
        }
    }

    override fun RhymeDrawScope.draw() {
        val line = text.ifEmpty { null } ?: return
        val measureResult = measureText(line, 72f)
        val textWidth = measureResult.width
        val position = Offset(960 - textWidth / 2, 0f)
        drawText(
            result = measureResult,
            position = position,
            color = Colors.White,
            shadow = Shadow(Colors.Dark, Offset(3f, 3f), 3f)
        )
        drawText(
            result = measureResult.clipWidth(textWidth * progress),
            position = position,
            color = Colors.Green4
        )
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
    private val lyricsBoard = LyricsBoard(lyrics)
    private val scoreBoard = ScoreBoard()
    private val comboBoard = ComboBoard()

    override fun update(frame: Int, position: Long) {
        lyricsBoard.update(frame, position)
        scoreBoard.update(frame, position)
        comboBoard.update(frame, position)
        track.update(frame, position)
        progressBoard.update(frame, position)
    }

    override fun RhymeDrawScope.draw() {
        with(lyricsBoard) { draw() }
        with(scoreBoard) { draw() }
        with(comboBoard) { draw() }
        with(track) { draw() }
        with(progressBoard) { draw() }
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