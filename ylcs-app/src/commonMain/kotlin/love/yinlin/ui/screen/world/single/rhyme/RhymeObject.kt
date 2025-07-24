package love.yinlin.ui.screen.world.single.rhyme

import androidx.collection.lruCache
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import love.yinlin.extension.roundToIntOffset
import love.yinlin.extension.translate
import kotlin.jvm.JvmInline
import kotlin.math.atan
import kotlin.random.Random

// 指针数据
internal data class Pointer(
    val startPosition: Offset,
    val startTime: Long,
    var position: Offset? = null,
    var time: Long? = null,
    var up: Boolean = false,
)

// 圆形矩形
@Stable
private class CircleRect(private val rect: Rect) {
    operator fun contains(point: Offset): Boolean {
        val a = rect.width / 2
        val b = rect.height / 2
        val dx = point.x - rect.left - a
        val dy = point.y - rect.top - b
        return (dx * dx) / (a * a) + (dy * dy) / (b * b) <= 1f
    }
}

private val Rect.circle get() = CircleRect(this)

private fun DrawScope.drawLine(color: Color, start: Offset, end: Offset, style: Stroke, alpha: Float = 1f) = this.drawLine(
    color = color,
    start = start,
    end = end,
    strokeWidth = style.width,
    cap = style.cap,
    pathEffect = style.pathEffect,
    alpha = alpha
)

private fun DrawScope.drawLine(brush: Brush, start: Offset, end: Offset, style: Stroke, alpha: Float = 1f) = this.drawLine(
    brush = brush,
    start = start,
    end = end,
    strokeWidth = style.width,
    cap = style.cap,
    pathEffect = style.pathEffect,
    alpha = alpha
)

private fun DrawScope.drawRect(color: Color, rect: Rect, alpha: Float = 1f) = this.drawRect(
    color = color,
    topLeft = rect.topLeft,
    size = rect.size,
    alpha = alpha
)

private fun DrawScope.drawRect(brush: Brush, rect: Rect, alpha: Float = 1f) = this.drawRect(
    brush = brush,
    topLeft = rect.topLeft,
    size = rect.size,
    alpha = alpha
)

private fun DrawScope.drawRoundRect(color: Color, rect: Rect, radius: Float, alpha: Float = 1f) = this.drawRoundRect(
    color = color,
    topLeft = rect.topLeft,
    size = rect.size,
    cornerRadius = CornerRadius(radius, radius),
    alpha = alpha
)

private fun DrawScope.drawRoundRect(brush: Brush, rect: Rect, radius: Float, alpha: Float = 1f) = this.drawRoundRect(
    brush = brush,
    topLeft = rect.topLeft,
    size = rect.size,
    cornerRadius = CornerRadius(radius, radius),
    alpha = alpha
)

private fun DrawScope.drawArc(color: Color, startAngle: Float, sweepAngle: Float, rect: Rect, alpha: Float = 1f, style: DrawStyle = Fill) = this.drawArc(
    color = color,
    startAngle = startAngle,
    sweepAngle = sweepAngle,
    useCenter = false,
    topLeft = rect.topLeft,
    size = rect.size,
    alpha = alpha,
    style = style
)

private fun DrawScope.drawArc(brush: Brush, startAngle: Float, sweepAngle: Float, rect: Rect, alpha: Float = 1f, style: DrawStyle = Fill) = this.drawArc(
    brush = brush,
    startAngle = startAngle,
    sweepAngle = sweepAngle,
    useCenter = false,
    topLeft = rect.topLeft,
    size = rect.size,
    alpha = alpha,
    style = style
)

private fun DrawScope.drawImage(image: ImageBitmap, position: Offset, size: Size) = this.drawImage(
    image = image,
    dstOffset = position.roundToIntOffset(),
    dstSize = size.roundToIntSize(),
    filterQuality = FilterQuality.High
)

private fun DrawScope.drawImage(image: ImageBitmap, rect: Rect) = this.drawImage(
    image = image,
    position = rect.topLeft,
    size = rect.size
)

private fun DrawScope.drawCircleImage(image: ImageBitmap, position: Offset, size: Size) = this.drawCircleImage(image, Rect(position, size))

private fun DrawScope.drawCircleImage(image: ImageBitmap, rect: Rect) = this.clipPath(Path().apply { addOval(rect) }) {
    this.drawImage(
        image = image,
        position = rect.topLeft,
        size = rect.size
    )
}

inline fun DrawScope.clipRect(position: Offset, size: Size, block: DrawScope.() -> Unit) = this.clipRect(
    left = position.x,
    top = position.y,
    right = (position.x + size.width),
    bottom = (position.y + size.height),
    block = block
)

inline fun DrawScope.clipRect(rect: Rect, block: DrawScope.() -> Unit) = this.clipRect(rect.topLeft, rect.size, block)

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

    fun DrawScope.drawText(
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

// 渲染实体
@Stable
private sealed interface RhymeObject {
    fun update(position: Long)
    fun DrawScope.draw(textManager: RhymeTextManager)
}

// 事件触发器
@Stable
private fun interface RhymeEvent {
    fun event(pointer: Pointer): Boolean
}

// 进度板
@Stable
private class ProgressBoard(
    private val duration: Long,
    private val record: ImageBitmap
) : RhymeObject, RhymeEvent {
    // 封面
    private val progressWidth = 8f
    private val recordSize = 64f
    private val recordCenter = Offset(960f, 360f)
    private val recordRect = Rect(recordCenter.translate(-recordSize, -recordSize), Size(recordSize * 2, recordSize * 2))
    private var recordAngle: Float = 0f

    // 游戏进度
    private var progress: Float by mutableFloatStateOf(0f)

    override fun update(position: Long) {
        progress = if (duration == 0L) 0f else (position / duration.toFloat()).coerceIn(0f, 1f)
        recordAngle += 20 / RhymeConfig.FPS.toFloat()
    }

    override fun event(pointer: Pointer): Boolean {
        if (pointer.up && pointer.startPosition in recordRect.circle) {
            println(pointer)
            return true
        }
        return false
    }

    override fun DrawScope.draw(textManager: RhymeTextManager) {
        // 画封面
        rotate(recordAngle, recordCenter) {
            drawCircleImage(image = record, rect = recordRect)
        }
        // 画时长
        drawArc(
            color = Colors.White,
            startAngle = -90f,
            sweepAngle = 360f,
            rect = recordRect,
            style = Stroke(width = progressWidth, cap = StrokeCap.Round)
        )
        // 画进度
        drawArc(
            color = Colors.Green4,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            rect = recordRect,
            style = Stroke(width = progressWidth, cap = StrokeCap.Round)
        )
    }
}

// 轨道
@Stable
private class Track(
    private val scoreBoard: ScoreBoard,
    private val comboBoard: ComboBoard
) : RhymeObject, RhymeEvent {
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

    // 当前按下轨道
    private val current: Int? by mutableStateOf(null)

    private fun DrawScope.drawTrackLine(start: Offset, end: Offset, width: Float) {
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

    override fun update(position: Long) {

    }

    override fun event(pointer: Pointer): Boolean {
        if (pointer.up) scoreBoard.addScore(Random.nextInt(1, 4))
        return true
    }

    override fun DrawScope.draw(textManager: RhymeTextManager) {
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
    private var progress by mutableFloatStateOf(0f)

    override fun update(position: Long) {
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

    override fun DrawScope.draw(textManager: RhymeTextManager) {
        val line = text.ifEmpty { null } ?: return
        textManager.apply {
            val textHeight = 72f
            val content = measureText(line, textHeight)
            val textWidth = content.width
            val position = Offset(960 - textWidth / 2, 0f)
            drawText(
                content = content,
                position = position,
                color = Colors.White,
                shadow = Shadow(Colors.Dark, Offset(3f, 3f), 3f)
            )
            clipRect(position, Size(textWidth * progress, textHeight)) {
                drawText(
                    content = content,
                    position = position,
                    color = Colors.Green4
                )
            }
        }
    }
}

// 分数板
@Stable
private class ScoreBoard : RhymeObject {
    // 七段数码管
    //     1
    // 6 ▎ ━  ▎ 2
    //   ▎ 7  ▎
    //   ▎ ━  ▎
    //   ▎    ▎
    // 5 ▎ ━  ▎ 3
    //     4
    @Stable
    @JvmInline
    value class Symbol private constructor(val value: Int) {
        companion object {
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

            fun fetchNumber(v: Byte): Int {
                for (i in 0 .. 9) {
                    if (NumberArray[i] == v) return i
                }
                return 0
            }
        }

        constructor(v1: Byte, v2: Byte = 0, v3: Byte = 127) : this(((v1.toInt() and 0xff) shl 16) or ((v2.toInt() and 0xff) shl 8) or (v3.toInt() and 0xff))

        val current: Byte get() = ((value shr 16) and 0xff).toByte()
        val target: Byte get() = ((value shr 8) and 0xff).toByte()
        val alpha: Byte get() = (value and 0xff).toByte()

        val isPlaying: Boolean get() = target.toInt() != 0
        val score: Int get() = fetchNumber(if (isPlaying) target else current)

        fun copy(v1: Byte = current, v2: Byte = target, v3: Byte = alpha): Symbol = Symbol(v1, v2, v3)

        override fun toString(): String = score.toString()
    }

    private val symbols = Array(4) {
        mutableStateOf(Symbol(Symbol.NumberArray[0]))
    }

    private val lock = SynchronizedObject()

    // 组合四个数位得分
    val score: Int get() {
        var factor = 10000
        return symbols.sumOf {
            factor /= 10
            it.value.score * factor
        }
    }

    // 增加得分
    fun addScore(value: Int) {
        synchronized(lock) {
            var newScore = score + value
            if (value < 1 || newScore > 9999) return
            for (index in 3 downTo 0) {
                var number by symbols[index]
                val data = Symbol.NumberArray[newScore % 10]
                if (number.isPlaying) {
                    if (data != number.target) number = number.copy(v2 = data, v3 = number.alpha)
                }
                else {
                    if (data != number.current) number = number.copy(v2 = data, v3 = 127)
                }
                newScore /= 10
            }
        }
    }

    override fun update(position: Long) {
        synchronized(lock) {
            symbols.forEach { symbol ->
                var number by symbol
                if (number.isPlaying) {
                    val alpha = number.alpha
                    number = if (alpha <= 0) number.copy(v1 = number.target, v2 = 0, v3 = 127)
                        else number.copy(v3 = (alpha - 600 / RhymeConfig.FPS).toByte().coerceIn(0, 127))
                }
            }
        }
    }

    private fun DrawScope.drawSymbol(mask: Int, v1: Int, v2: Int, a: Float, rect: Rect) {
        val b1 = (v1 and mask) == mask
        val b2 = (v2 and mask) == mask
        if (b1) {
            if (b2) drawRoundRect(Colors.Red4, rect, 5f, 1f)
            else drawRoundRect(Colors.Red4, rect, 5f, a)
        }
        else if (b2) drawRoundRect(Colors.Red4, rect, 5f, 1 - a)
    }

    override fun DrawScope.draw(textManager: RhymeTextManager) {
        val position = Offset(640f, 100f)
        withTransform({
            translate(position.x, position.y)
            rotateRad(atan(288 / 768f), Offset.Zero)
        }) {
            var x = 0f
            symbols.forEach { symbol ->
                translate(left = x) {
                    symbol.value.apply {
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
                x += 70f
            }
        }
    }
}

// 连击板
@Stable
private class ComboBoard : RhymeObject {
    @Stable
    private enum class ActionResult {
        PERFECT, GOOD, MISS
    }

    private var result by mutableStateOf<ActionResult?>(null)
    private var combo by mutableIntStateOf(0)

    override fun update(position: Long) {
        
    }

    override fun DrawScope.draw(textManager: RhymeTextManager) {

    }
}

// 场景
@Stable
private class Scene(
    lyrics: RhymeLyricsConfig,
    record: ImageBitmap
) : RhymeObject, RhymeEvent {
    val progressBoard = ProgressBoard(lyrics.duration, record)
    val lyricsBoard = LyricsBoard(lyrics)
    val scoreBoard = ScoreBoard()
    val comboBoard = ComboBoard()
    val track = Track(scoreBoard, comboBoard)

    override fun update(position: Long) {
        lyricsBoard.update(position)
        scoreBoard.update(position)
        comboBoard.update(position)
        track.update(position)
        progressBoard.update(position)
    }

    override fun event(pointer: Pointer): Boolean = progressBoard.event(pointer) || track.event(pointer)

    override fun DrawScope.draw(textManager: RhymeTextManager) {
        lyricsBoard.apply { draw(textManager) }
        scoreBoard.apply { draw(textManager) }
        comboBoard.apply { draw(textManager) }
        track.apply { draw(textManager) }
        progressBoard.apply { draw(textManager) }
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
        scene?.update(position)
    }

    fun onEvent(pointer: Pointer) {
        scene?.event(pointer)
    }

    fun onDraw(scope: DrawScope, textManager: RhymeTextManager) {
        scene?.apply { scope.draw(textManager) }
    }

    fun onResult(): RhymeResult {
        return RhymeResult(0)
    }
}