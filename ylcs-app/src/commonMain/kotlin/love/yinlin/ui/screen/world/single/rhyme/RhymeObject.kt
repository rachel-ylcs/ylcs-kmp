package love.yinlin.ui.screen.world.single.rhyme

import androidx.collection.lruCache
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.util.*
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import love.yinlin.common.Colors
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.extension.*
import kotlin.math.atan
import kotlin.random.Random

private val Size.Companion.Game get() = Size(1920f, 1080f)

// 指针数据
internal data class Pointer(
    val startPosition: Offset,
    val startTime: Long,
    var position: Offset? = null,
    var time: Long? = null,
    var up: Boolean = false,
)

// 文本绘制管理器
@Stable
internal data class RhymeTextManager(
    private val font: Font,
    private val fontFamilyResolver: FontFamily.Resolver
) {
    @Stable
    private data class CacheKey(
        val text: String,
        val height: Float,
        val fontWeight: FontWeight
    )

    private val lruCache = lruCache<CacheKey, Paragraph>(16)

    fun measureText(text: String, height: Float, fontWeight: FontWeight = FontWeight.Light): Paragraph {
        // 查询缓存
        val cacheKey = CacheKey(text, height, fontWeight)
        val cacheResult = lruCache[cacheKey]
        if (cacheResult != null) return cacheResult

        val intrinsics = ParagraphIntrinsics(
            text = text,
            style = TextStyle(
                fontSize = TextUnit(height / 1.17f, TextUnitType.Sp),
                fontWeight = fontWeight,
                fontFamily = FontFamily(font)
            ),
            annotations = emptyList(),
            density = Density(1f),
            fontFamilyResolver = fontFamilyResolver,
            placeholders = emptyList()
        )

        return Paragraph(
            paragraphIntrinsics = intrinsics,
            constraints = Constraints.fitPrioritizingWidth(minWidth = 0, maxWidth = intrinsics.maxIntrinsicWidth.toInt(), minHeight = 0, maxHeight = height.toInt()),
            maxLines = 1,
            overflow = TextOverflow.Clip
        ).also { lruCache.put(cacheKey, it) }
    }

    fun DrawScope.text(
        content: Paragraph,
        position: Offset,
        color: Color,
        shadow: Shadow? = null,
        decoration: TextDecoration? = null,
        drawStyle: DrawStyle? = null,
        blendMode: BlendMode = DrawScope.DefaultBlendMode
    ) {
        withTransform({
            translate(left = position.x, top = position.y)
            clipRect(left = 0f, top = 0f, right = content.width, bottom = content.height)
        }) {
            content.paint(
                canvas = drawContext.canvas,
                color = color,
                shadow = shadow,
                textDecoration = decoration,
                drawStyle = drawStyle,
                blendMode = blendMode
            )
        }
    }
}

// 容器
@Stable
private sealed interface RhymeContainer {
    fun contains(position: Offset, size: Size, point: Offset): Boolean

    @Stable
    interface Rectangle : RhymeContainer {
        override fun contains(position: Offset, size: Size, point: Offset): Boolean {
            val x = point.x
            val y = point.y
            val left = position.x
            val top = position.y
            return (x >= left) and (x < left + size.width) and (y >= top) and (y < top + size.height)
        }
    }

    @Stable
    interface Circle : RhymeContainer {
        override fun contains(position: Offset, size: Size, point: Offset): Boolean {
            val a = size.width / 2
            val b = size.height / 2
            val dx = point.x - position.x - a
            val dy = point.y - position.y - b
            return (dx * dx) / (a * a) + (dy * dy) / (b * b) <= 1f
        }
    }
}

// 事件触发器
@Stable
private fun interface RhymeEvent {
    fun onEvent(pointer: Pointer): Boolean
}

// 渲染实体
@Stable
private sealed class RhymeObject : RhymeContainer {
    abstract val position: Offset
    abstract val size: Size
    open val transform: (DrawTransform.() -> Unit)? = null

    protected abstract fun DrawScope.onDraw(textManager: RhymeTextManager)

    operator fun contains(point: Offset): Boolean = contains(position, size, point)

    fun DrawScope.draw(textManager: RhymeTextManager) {
        withTransform({
            translate(left = position.x, top = position.y)
            transform?.invoke(this)
        }) {
            onDraw(textManager)
        }
    }

    fun DrawScope.line(color: Color, start: Offset, end: Offset, style: Stroke, alpha: Float = 1f) =
        this.drawLine(color = color, start = start, end = end, strokeWidth = style.width, cap = style.cap, pathEffect = style.pathEffect, alpha = alpha)

    fun DrawScope.line(brush: Brush, start: Offset, end: Offset, style: Stroke, alpha: Float = 1f) =
        this.drawLine(brush = brush, start = start, end = end, strokeWidth = style.width, cap = style.cap, pathEffect = style.pathEffect, alpha = alpha)

    fun DrawScope.rect(color: Color, position: Offset = Offset.Zero, size: Size = this@RhymeObject.size, alpha: Float = 1f) =
        this.drawRect(color = color, topLeft = position, size = size, alpha = alpha)

    fun DrawScope.rect(brush: Brush, position: Offset = Offset.Zero, size: Size = this@RhymeObject.size, alpha: Float = 1f) =
        this.drawRect(brush = brush, topLeft = position, size = size, alpha = alpha)

    fun DrawScope.roundRect(color: Color, radius: Float, position: Offset = Offset.Zero, size: Size = this@RhymeObject.size, alpha: Float = 1f) =
        this.drawRoundRect(color = color, topLeft = position, size = size, cornerRadius = CornerRadius(radius, radius), alpha = alpha)

    fun DrawScope.roundRect(brush: Brush, radius: Float, position: Offset = Offset.Zero, size: Size = this@RhymeObject.size, alpha: Float = 1f) =
        this.drawRoundRect(brush = brush, topLeft = position, size = size, cornerRadius = CornerRadius(radius, radius), alpha = alpha)

    fun DrawScope.arc(color: Color, startAngle: Float, sweepAngle: Float, position: Offset = Offset.Zero, size: Size = this@RhymeObject.size, alpha: Float = 1f, style: DrawStyle = Fill) =
        this.drawArc(color = color, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, topLeft = position, size = size, alpha = alpha, style = style)

    fun DrawScope.arc(brush: Brush, startAngle: Float, sweepAngle: Float, position: Offset = Offset.Zero, size: Size = this@RhymeObject.size, alpha: Float = 1f, style: DrawStyle = Fill) =
        this.drawArc(brush = brush, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, topLeft = position, size = size, alpha = alpha, style = style)

    fun DrawScope.image(image: ImageBitmap, position: Offset = Offset.Zero, size: Size = this@RhymeObject.size) =
        this.drawImage(image = image, dstOffset = position.roundToIntOffset(), dstSize = size.roundToIntSize(), filterQuality = FilterQuality.High)

    fun DrawScope.circleImage(image: ImageBitmap, position: Offset = Offset.Zero, size: Size = this@RhymeObject.size) =
        this.clipPath(Path().apply { addOval(Rect(position, size)) }) { this.image(image = image, position = position, size = size) }

    inline fun DrawScope.clip(position: Offset = Offset.Zero, size: Size = this@RhymeObject.size, block: DrawScope.() -> Unit) =
        this.clipRect(left = position.x, top = position.y, right = (position.x + size.width), bottom = (position.y + size.height), block = block)
}

// 动态实体
@Stable
private abstract class RhymeDynamic : RhymeObject() {
    abstract fun onUpdate(position: Long)
}

// 进度板
@Stable
private class ProgressBoard(
    center: Offset,
    private val duration: Long,
    private val record: ImageBitmap
) : RhymeDynamic(), RhymeContainer.Circle, RhymeEvent {
    private val stroke = 8f
    private val radius = 64f
    override val position: Offset = center.translate(-radius, -radius)
    override val size: Size = Size(radius * 2, radius * 2)

    // 封面旋转角
    var angle: Float by mutableFloatStateOf(0f)
    // 游戏进度
    var progress: Float by mutableFloatStateOf(0f)

    override fun onUpdate(position: Long) {
        progress = if (duration == 0L) 0f else (position / duration.toFloat()).coerceIn(0f, 1f)
        angle += 20 / RhymeConfig.FPS.toFloat()
    }

    override fun onEvent(pointer: Pointer): Boolean {
        if (pointer.up && pointer.startPosition in this) {
            println(pointer)
            return true
        }
        return false
    }

    override fun DrawScope.onDraw(textManager: RhymeTextManager) {
        // 画封面
        rotate(angle, Offset(radius, radius)) { circleImage(record) }
        // 画时长
        arc(Colors.White, -90f, 360f, style = Stroke(width = stroke, cap = StrokeCap.Round))
        // 画进度
        arc(Colors.Green4, -90f, 360f * progress, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

// 音符板
@Stable
private class NoteBoard(
    center: Offset,
    scoreBoard: ScoreBoard,
    comboBoard: ComboBoard
) : RhymeDynamic(), RhymeContainer.Rectangle, RhymeEvent {
    @Stable
    class Track(
        center: Offset,
        private val scoreBoard: ScoreBoard,
        private val comboBoard: ComboBoard
    ) : RhymeDynamic(), RhymeContainer.Rectangle, RhymeEvent {
        override val position: Offset = Offset.Zero
        override val size: Size = Size.Game

        private val trackWidth = 20f
        private val tracks = listOf(
            Offset(0f, 0f),
            Offset(0f, size.height / 2),
            Offset(0f, size.height),
            Offset(size.width / 3, size.height),
            Offset(size.width * 2 / 3, size.height),
            Offset(size.width, size.height),
            Offset(size.width, size.height / 2),
            Offset(size.width, 0f),
        ).map { center to it }

        private val current: Int? by mutableStateOf(null)

        override fun onUpdate(position: Long) {

        }

        override fun onEvent(pointer: Pointer): Boolean {
            if (pointer.up) scoreBoard.addScore(Random.nextInt(1, 4))
            return true
        }

        private fun DrawScope.drawTrackLine(start: Offset, end: Offset, width: Float) {
            // 阴影
            repeat(5) {
                line(Colors.Steel3, start, end, Stroke(width = width * (1.15f + it * 0.15f), cap = StrokeCap.Round), 0.15f - (it * 0.03f))
            }
            // 光带
            line(Colors.Steel4, start, end, Stroke(width = width, cap = StrokeCap.Round), 0.7f)
            // 高光
            line(Colors.White, start, end, Stroke(width = width * 0.8f, cap = StrokeCap.Round), 0.8f)
        }

        override fun DrawScope.onDraw(textManager: RhymeTextManager) {
            // 画轨道射线
            for ((start, end) in tracks) drawTrackLine(start = start, end = end, width = trackWidth)
        }
    }

    override val position: Offset = Offset.Zero
    override val size: Size = Size.Game

    private val track = Track(center, scoreBoard, comboBoard)

    override fun onUpdate(position: Long) {
        track.run { onUpdate(position) }
    }

    override fun onEvent(pointer: Pointer): Boolean = track.run { onEvent(pointer) }

    override fun DrawScope.onDraw(textManager: RhymeTextManager) {
        track.run { draw(textManager) }
    }
}

// 歌词板
@Stable
private class LyricsBoard(
    private val lyrics: RhymeLyricsConfig
) : RhymeDynamic(), RhymeContainer.Rectangle {
    private val textHeight: Float = 72f
    override val position: Offset = Offset.Zero
    override val size: Size = Size(Size.Game.width, textHeight)

    private var currentIndex = -1
    private var text by mutableStateOf("")
    private var progress by mutableFloatStateOf(0f)

    override fun onUpdate(position: Long) {
        val lines = lyrics.lyrics
        val nextLine = lines.getOrNull(currentIndex + 1)
        if (nextLine != null && position >= nextLine.start) {
            ++currentIndex
            // 合并字符显示 (因为 plain text 内可能包含不是 Action 的空白字符)
            text = nextLine.theme.fastJoinToString("") { it.ch }
        } else if (progress >= 1f) return // 优化句间停顿

        val line = lines.getOrNull(currentIndex) ?: return
        val theme = line.theme
        var currentLength = 0f
        val totalLength = text.length
        if (theme.size != totalLength) return

        for (i in theme.indices) {
            val action = theme[i]
            val length = action.ch.length
            if (position > line.start + action.end) currentLength += length
            else {
                val start = theme.getOrNull(i - 1)?.end ?: 0
                currentLength += length * (position - line.start - start) / (action.end - start).toFloat()
                break
            }
        }
        progress = (currentLength / totalLength).coerceIn(0f, 1f)
    }

    override fun DrawScope.onDraw(textManager: RhymeTextManager) {
        val line = text.ifEmpty { null } ?: return
        textManager.run {
            val content = measureText(line, textHeight)
            val textWidth = content.width
            val start = Offset((this@LyricsBoard.size.width - textWidth) / 2, 0f)
            text(
                content = content,
                position = start,
                color = Colors.White,
                shadow = Shadow(Colors.Dark, Offset(3f, 3f), 3f)
            )
            clip(start, Size(textWidth * progress, textHeight)) {
                text(
                    content = content,
                    position = start,
                    color = Colors.Green4
                )
            }
        }
    }
}

// 分数板
@Stable
private class ScoreBoard : RhymeDynamic(), RhymeContainer.Rectangle {
    @Stable
    private class ScoreNumber(p: Offset) : RhymeDynamic(), RhymeContainer.Rectangle {
        // 七段数码管
        //     1
        // 6 ▎ ━  ▎ 2
        //   ▎ 7  ▎
        //   ▎ ━  ▎
        //   ▎    ▎
        // 5 ▎ ━  ▎ 3
        //     4
        companion object {
            const val WIDTH = 60f
            const val HEIGHT = 110f
            const val GAP = 10f
            val NumberArray = byteArrayOf(
                (1 + 2 + 4 + 8 + 16 + 32 + 0).toByte(),
                (0 + 2 + 4 + 0 + 0 + 0 + 0).toByte(),
                (1 + 2 + 0 + 8 + 16 + 0 + 64).toByte(),
                (1 + 2 + 4 + 8 + 0 + 0 + 64).toByte(),
                (0 + 2 + 4 + 0 + 0 + 32 + 64).toByte(),
                (1 + 0 + 4 + 8 + 0 + 32 + 64).toByte(),
                (1 + 0 + 4 + 8 + 16 + 32 + 64).toByte(),
                (1 + 2 + 4 + 0 + 0 + 0 + 0).toByte(),
                (1 + 2 + 4 + 8 + 16 + 32 + 64).toByte(),
                (1 + 2 + 4 + 8 + 0 + 32 + 64).toByte()
            )

            private fun fetchNumber(v: Byte): Int {
                for (i in 0 .. 9) {
                    if (NumberArray[i] == v) return i
                }
                return 0
            }

            private fun encode(v1: Byte, v2: Byte = 0, v3: Byte = 127): Int = ((v1.toInt() and 0xff) shl 16) or ((v2.toInt() and 0xff) shl 8) or (v3.toInt() and 0xff)
        }

        override val position: Offset = p
        override val size: Size = Size(WIDTH, HEIGHT)

        private var data by mutableIntStateOf(encode(NumberArray[0]))

        val current: Byte get() = ((data shr 16) and 0xff).toByte()
        val target: Byte get() = ((data shr 8) and 0xff).toByte()
        val alpha: Byte get() = (data and 0xff).toByte()
        val isPlaying: Boolean get() = target.toInt() != 0
        val score: Int get() = fetchNumber(if (isPlaying) target else current)

        fun reset(v1: Byte = current, v2: Byte = target, v3: Byte = alpha) {
            data = encode(v1, v2, v3)
        }

        override fun onUpdate(position: Long) {
            val v2 = target
            if (v2.toInt() != 0) {
                val a = alpha
                if (a <= 0) reset(v1 = v2, v2 = 0, v3 = 127)
                else reset(v3 = (a - 600 / RhymeConfig.FPS).toByte().coerceIn(0, 127))
            }
        }

        private fun DrawScope.drawSymbol(mask: Int, v1: Int, v2: Int, a: Float, rect: Rect) {
            val b1 = (v1 and mask) == mask
            val b2 = (v2 and mask) == mask
            if (b1) {
                if (b2) roundRect(Colors.Red4, 5f, rect.topLeft, rect.size, 1f)
                else roundRect(Colors.Red4, 5f, rect.topLeft, rect.size, a)
            }
            else if (b2) roundRect(Colors.Red4, 5f, rect.topLeft, rect.size, 1 - a)
        }

        override fun DrawScope.onDraw(textManager: RhymeTextManager) {
            val a = (alpha / 127f).coerceIn(0f, 1f)
            val v1 = current.toInt()
            val v2 = target.toInt()
            drawSymbol(1, v1, v2, a, Rect(10f, 0f, 50f, 10f))
            drawSymbol(2, v1, v2, a, Rect(50f, 10f, 60f, 50f))
            drawSymbol(4, v1, v2, a, Rect(50f, 60f, 60f, 100f))
            drawSymbol(8, v1, v2, a, Rect(10f, 100f, 50f, 110f))
            drawSymbol(16, v1, v2, a, Rect(0f, 60f, 10f, 100f))
            drawSymbol(32, v1, v2, a, Rect(0f, 10f, 10f, 50f))
            drawSymbol(64, v1, v2, a, Rect(10f, 50f, 50f, 60f))
        }
    }

    override val position: Offset = Offset(Size.Game.width / 3, 100f)
    override val size: Size = Size(ScoreNumber.WIDTH * 4 + ScoreNumber.GAP * 3, ScoreNumber.HEIGHT)
    override val transform: (DrawTransform.() -> Unit) = {
        rotateRad(atan(288 / 768f), Offset.Zero)
    }

    private val numbers = Array(4) {
        ScoreNumber(Offset(it * (ScoreNumber.WIDTH + ScoreNumber.GAP), 0f))
    }

    private val lock = SynchronizedObject()

    // 组合四个数位得分
    val score: Int get() {
        var factor = 10000
        return numbers.sumOf {
            factor /= 10
            it.score * factor
        }
    }

    // 增加得分
    fun addScore(value: Int) {
        synchronized(lock) {
            var newScore = score + value
            if (value < 1 || newScore > 9999) return
            for (index in 3 downTo 0) {
                val number = numbers[index]
                val data = ScoreNumber.NumberArray[newScore % 10]
                if (number.isPlaying) {
                    if (data != number.target) number.reset(v2 = data, v3 = number.alpha)
                }
                else {
                    if (data != number.current) number.reset(v2 = data, v3 = 127)
                }
                newScore /= 10
            }
        }
    }

    override fun onUpdate(position: Long) {
        synchronized(lock) {
            for (number in numbers) number.onUpdate(position)
        }
    }

    override fun DrawScope.onDraw(textManager: RhymeTextManager) {
        for (number in numbers) number.run { draw(textManager) }
    }
}

// 连击板
@Stable
private class ComboBoard : RhymeDynamic(), RhymeContainer.Rectangle {
    override val position: Offset = Offset.Zero
    override val size: Size = Size.Game

    @Stable
    private enum class ActionResult {
        PERFECT, GOOD, MISS
    }

    private var result by mutableStateOf<ActionResult?>(null)
    private var combo by mutableIntStateOf(0)

    override fun onUpdate(position: Long) {
        
    }

    override fun DrawScope.onDraw(textManager: RhymeTextManager) {

    }
}

// 场景
@Stable
private class Scene(
    lyrics: RhymeLyricsConfig,
    record: ImageBitmap
) : RhymeDynamic(), RhymeContainer.Rectangle, RhymeEvent {
    override val position: Offset = Offset.Zero
    override val size: Size = Size.Game

    private val center: Offset = Offset(size.width / 2, 360f)

    val progressBoard = ProgressBoard(center, lyrics.duration, record)
    val lyricsBoard = LyricsBoard(lyrics)
    val scoreBoard = ScoreBoard()
    val comboBoard = ComboBoard()
    val noteBoard = NoteBoard(center, scoreBoard, comboBoard)

    override fun onUpdate(position: Long) {
        lyricsBoard.onUpdate(position)
        scoreBoard.onUpdate(position)
        comboBoard.onUpdate(position)
        noteBoard.onUpdate(position)
        progressBoard.onUpdate(position)
    }

    override fun onEvent(pointer: Pointer): Boolean = progressBoard.onEvent(pointer) || noteBoard.onEvent(pointer)

    override fun DrawScope.onDraw(textManager: RhymeTextManager) {
        lyricsBoard.run { draw(textManager) }
        scoreBoard.run { draw(textManager) }
        comboBoard.run { draw(textManager) }
        noteBoard.run { draw(textManager) }
        progressBoard.run { draw(textManager) }
    }
}

// 游戏舞台
@Stable
internal class RhymeStage {
    private var scene: Scene? = null

    fun onInitialize(lyrics: RhymeLyricsConfig, record: ImageBitmap, speed: Int) {
        scene = Scene(lyrics, record)
    }

    fun onClear() {
        scene = null
    }

    fun onUpdate(position: Long) {
        scene?.onUpdate(position)
    }

    fun onEvent(pointer: Pointer) {
        scene?.onEvent(pointer)
    }

    fun onDraw(scope: DrawScope, textManager: RhymeTextManager) {
        scene?.run { scope.draw(textManager) }
    }

    fun onResult(): RhymeResult {
        return RhymeResult(0)
    }
}