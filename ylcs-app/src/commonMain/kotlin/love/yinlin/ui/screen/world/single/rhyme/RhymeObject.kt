package love.yinlin.ui.screen.world.single.rhyme

import androidx.collection.MutableLongObjectMap
import androidx.collection.mutableLongObjectMapOf
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.*
import androidx.compose.ui.util.*
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import love.yinlin.common.Colors
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.extension.roundToIntOffset
import love.yinlin.extension.translate
import kotlin.jvm.JvmInline
import kotlin.math.*

private operator fun Rect.times(scale: Float): Rect = Rect(left * scale, top * scale, right * scale, bottom * scale)

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

// 指针数据
@Stable
private data class PointerData(
    val id: Long,
    val start: Offset,
    var move: Offset? = null,
    var end: Offset? = null
) {
    override fun toString(): String = "[$id] ${
        end?.let { "(${start.x}, ${start.y}) -> (${it.x}, ${it.y})" } ?:
        move?.let { "(${start.x}, ${start.y}) | (${it.x}, ${it.y})" } ?:
        "(${start.x}, ${start.y})"
    }"
}

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
        fun addOval(rect: Rect): RhymePath {
            path.addOval(rect * scale)
            return this
        }
        fun addRect(rect: Rect): RhymePath {
            path.addRect(rect * scale)
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

    fun drawRect(color: Color, rect: Rect, alpha: Float = 1f, style: DrawStyle = Fill) =
        drawRect(color, rect.topLeft, rect.size, alpha, style)

    fun drawRect(brush: Brush, rect: Rect, alpha: Float = 1f, style: DrawStyle = Fill) =
        drawRect(brush, rect.topLeft, rect.size, alpha, style)

    fun drawCircle(color: Color, radius: Float, center: Offset, alpha: Float = 1f, style: DrawStyle = Fill) =
        scope.drawCircle(color, radius * scale, center * scale, alpha, style * scale)

    fun drawCircle(brush: Brush, radius: Float, center: Offset, alpha: Float = 1f, style: DrawStyle = Fill) =
        scope.drawCircle(brush, radius * scale, center * scale, alpha, style * scale)

    fun drawArc(color: Color, startAngle: Float, sweepAngle: Float, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill) =
        scope.drawArc(color, startAngle, sweepAngle, false, position * scale, size * scale, alpha, style * scale)

    fun drawArc(brush: Brush, startAngle: Float, sweepAngle: Float, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill) =
        scope.drawArc(brush, startAngle, sweepAngle, false, position * scale, size * scale, alpha, style * scale)

    fun drawArc(color: Color, startAngle: Float, sweepAngle: Float, rect: Rect, alpha: Float = 1f, style: DrawStyle = Fill) =
        drawArc(color, startAngle, sweepAngle, rect.topLeft, rect.size, alpha, style)

    fun drawArc(brush: Brush, startAngle: Float, sweepAngle: Float, rect: Rect, alpha: Float = 1f, style: DrawStyle = Fill) =
        drawArc(brush, startAngle, sweepAngle, rect.topLeft, rect.size, alpha, style)

    fun drawPath(color: Color, path: RhymePath, alpha: Float = 1f, style: DrawStyle = Fill) =
        scope.drawPath(path.path, color, alpha, style * scale)

    fun drawPath(brush: Brush, path: RhymePath, alpha: Float = 1f, style: DrawStyle = Fill) =
        scope.drawPath(path.path, brush, alpha, style * scale)

    fun measureText(text: String, expectHeight: Float, fill: Boolean = true): RhymeMeasureResult {
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
            style = if (fill) TextStyle(
                fontSize = TextUnit(actualTextSize, TextUnitType.Sp),
                fontFamily = FontFamily(textData.font)
            ) else TextStyle(
                brush = null,
                fontSize = TextUnit(actualTextSize, TextUnitType.Sp),
                fontFamily = FontFamily(textData.font),
                shadow = null
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

    fun drawImage(image: ImageBitmap, rect: Rect) = drawImage(image, rect.topLeft, rect.size)

    inline fun clipRect(position: Offset, size: Size, block: RhymeDrawScope.() -> Unit) =
        scope.clipRect(position.x * scale, position.y * scale, (position.x + size.width) * scale, (position.y + size.height) * scale) { block() }

    inline fun clipRect(rect: Rect, block: RhymeDrawScope.() -> Unit) = clipRect(rect.topLeft, rect.size, block)

    inline fun clipPath(path: RhymePath, block: RhymeDrawScope.() -> Unit) =
        scope.clipPath(path.path) { block() }

    inline fun rotate(degree: Float, pivot: Offset, block: RhymeDrawScope.() -> Unit) = scope.rotate(degree, pivot * scale) { block() }
    inline fun rotateRad(radian: Float, pivot: Offset, block: RhymeDrawScope.() -> Unit) = scope.rotateRad(radian, pivot * scale) { block() }
}

// 渲染实体
@Stable
private sealed interface RhymeObject {
    fun update(frame: Int, position: Long)
    fun RhymeDrawScope.draw()
}

// 事件触发器
@Stable
private fun interface RhymeEvent {
    fun event(pointer: PointerData): Boolean
}

// 进度板
@Stable
private class ProgressBoard(
    private val duration: Long,
    private val record: ImageBitmap
) : RhymeObject, RhymeEvent {
    // 封面
    private val progressWidth = 8f
    private val recordRect = Rect(Offset(960f - 64f, 360f - 64f), Size(128f, 128f))

    // 游戏进度
    private var progress: Float by mutableFloatStateOf(0f)

    override fun update(frame: Int, position: Long) {
        progress = if (duration == 0L) 0f else (position / duration.toFloat()).coerceIn(0f, 1f)
    }

    override fun event(pointer: PointerData): Boolean {
        if (pointer.end != null && pointer.start in recordRect.circle) {
            println(pointer)
            return true
        }
        return false
    }

    override fun RhymeDrawScope.draw() {
        // 画封面
        clipPath(RhymePath().addOval(recordRect)) {
            drawImage(image = record, rect = recordRect)
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
private class Track : RhymeObject, RhymeEvent {
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

    override fun event(pointer: PointerData): Boolean = false

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
        val nextLine = lines.getOrNull(currentIndex + 1)
        if (nextLine != null && position >= nextLine.start) {
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
    companion object {
        // 动画帧数 (平均字符时长 250 毫秒)
        private const val FPA: Byte = (250 * RhymeConfig.FPS / 1000).toByte()
        // 非数
        private const val ILLEGAL_NUMBER: Byte = -1
    }

    @Stable
    @JvmInline
    private value class ScoreNumber(val value: Int) {
        // 当前帧
        val frame: Byte get() = (value ushr 24).toByte()
        // 旧数字 (消失)
        val oldNumber: Byte get() = ((value ushr 16) and 0xff).toByte()
        // 新数字 (出现)
        val newNumber: Byte get() = ((value ushr 8) and 0xff).toByte()
        // 缓冲区下一个数字
        val nextNumber: Byte get() = (value and 0xff).toByte()

        // 实际值 (优先级: nextNumber > newNumber > oldNumber)
        val actualNumber: Int get() {
            val n1 = oldNumber
            val n2 = newNumber
            val n3 = nextNumber
            return (if (n3 != ILLEGAL_NUMBER) n3 else if (n2 != ILLEGAL_NUMBER) n2 else n1).toInt()
        }

        constructor(frame: Byte = FPA, oldNumber: Byte, newNumber: Byte = ILLEGAL_NUMBER, nextNumber: Byte = ILLEGAL_NUMBER) : this(
            value = (frame.toInt() shl 24) or ((oldNumber.toInt() and 0xff) shl 16) or ((newNumber.toInt() and 0xff) shl 8) or (nextNumber.toInt() and 0xff)
        )

        fun copy(frame: Byte = this.frame, oldNumber: Byte = this.oldNumber, newNumber: Byte = this.newNumber, nextNumber: Byte = this.nextNumber) =
            ScoreNumber(frame = frame, oldNumber = oldNumber, newNumber = newNumber, nextNumber = nextNumber)

        // 设置数位 (number in 1 .. 9)
        fun reset(number: Byte): ScoreNumber? {
            val n1 = oldNumber
            val n2 = newNumber
            val n3 = nextNumber
            return if (n3 != ILLEGAL_NUMBER) { // 缓冲区有数字
                // 如果正在替换的数字与新数字的相同, 则缓冲区不重要了直接清空
                if (n2 == number) copy(nextNumber = ILLEGAL_NUMBER)
                // 如果缓冲区数字与新数字不同, 则更新缓冲区
                else if (n3 != number) copy(nextNumber = number)
                else null
            }
            else if (n2 != ILLEGAL_NUMBER) { // 正在替换数字
                // 如果正在替换数字和新数字不同则将新数字放入缓冲区
                if (n2 != number) copy(nextNumber = number)
                else null
            }
            else if (n1 != number) copy(newNumber = number, frame = 0) // 旧数字与新数字不同, 启动替换
            else null
        }

        // 更新数位
        fun update(): ScoreNumber? {
            val f = frame
            return if (f >= FPA - 1) { // 帧数到达峰值
                val n2 = newNumber
                val n3 = nextNumber
                // 缓冲区有数字: 新数字替换成缓冲区数字 启动新数字 清空缓冲区 重置帧
                if (n3 != ILLEGAL_NUMBER) copy(oldNumber = n2, newNumber = n3, nextNumber = ILLEGAL_NUMBER, frame = 0)
                // 缓冲区无数字: 结束 清空新数字和缓冲区
                else if (n2 != ILLEGAL_NUMBER) copy(oldNumber = n2, newNumber = ILLEGAL_NUMBER, nextNumber = ILLEGAL_NUMBER, frame = FPA)
                else null
            }
            else copy(frame = (f + 1).toByte()) // 切换帧
        }

        fun RhymeDrawScope.drawNumber(position: Offset, index: Int, height: Float) {
            val f = frame
            val oldResult = measureText(oldNumber.toString(), height, true)
            val strokeOldResult = measureText(oldNumber.toString(), height, false)
            val numberWidth = oldResult.width
            val numberPosition = position.translate(x = index * numberWidth)
            if (f >= FPA) {
                drawText(
                    result = oldResult,
                    position = numberPosition,
                    color = Colors.Steel4
                )
                drawText(
                    result = strokeOldResult,
                    position = numberPosition,
                    color = Colors.White,
                    drawStyle = Stroke(width = 3f, join = StrokeJoin.Round)
                )
            }
            else {
                val newResult = measureText(newNumber.toString(), height, true)
                val strokeNewResult = measureText(newNumber.toString(), height, false)
                val top = height * f / FPA
                val bottom = height - top
                val alpha = (f / FPA.toFloat()).coerceIn(0f, 1f)
                drawText(
                    result = oldResult.clipHeight(bottom),
                    position = numberPosition,
                    color = Colors.Steel4,
                    alpha = 1 - alpha
                )
                drawText(
                    result = strokeOldResult.clipHeight(bottom),
                    position = numberPosition,
                    color = Colors.White,
                    drawStyle = Stroke(width = 3f, join = StrokeJoin.Round),
                    alpha = 1 - alpha
                )
                clipRect(numberPosition.translate(y = bottom), Size(numberWidth, top)) {
                    drawText(
                        result = newResult,
                        position = numberPosition,
                        color = Colors.Steel4,
                        alpha = alpha
                    )
                    drawText(
                        result = strokeNewResult,
                        position = numberPosition,
                        color = Colors.White,
                        drawStyle = Stroke(width = 3f, join = StrokeJoin.Round),
                        alpha = alpha
                    )
                }
            }
        }

        override fun toString(): String {
            val n1 = oldNumber
            val n2 = newNumber
            return "[$frame: ${if (n2 != ILLEGAL_NUMBER) "$n1 -> $n2" else n1} | $nextNumber]"
        }
    }

    private val numbers = Array(4) { mutableStateOf(ScoreNumber(oldNumber = 0)) }
    private val lock = SynchronizedObject()

    // 组合四个数位得分
    private val score: Int get() {
        var factor = 10000
        return numbers.sumOf {
            factor /= 10
            it.value.actualNumber * factor
        }
    }

    // 增加得分
    fun addScore(value: Int) {
        synchronized(lock) {
            var newScore = score + value
            if (value < 1 || newScore > 9999) return
            for (index in numbers.size - 1 downTo 0) {
                var number by numbers[index]
                number.reset((newScore % 10).toByte())?.let { number = it }
                newScore /= 10
            }
        }
    }

    override fun update(frame: Int, position: Long) {
        synchronized(lock) {
            numbers.forEach { number ->
                number.value.update()?.let { number.value = it }
            }
        }
    }

    override fun RhymeDrawScope.draw() {
        val position = Offset(620f, 80f)
        rotateRad(atan(288 / 768f), position) {
            numbers.forEachIndexed { index, number ->
                number.value.apply { drawNumber(position, index, 140f) }
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
) : RhymeObject, RhymeEvent {
    val progressBoard = ProgressBoard(lyrics.duration, record)
    val track = Track()
    val lyricsBoard = LyricsBoard(lyrics)
    val scoreBoard = ScoreBoard()
    val comboBoard = ComboBoard()

    override fun update(frame: Int, position: Long) {
        lyricsBoard.update(frame, position)
        scoreBoard.update(frame, position)
        comboBoard.update(frame, position)
        track.update(frame, position)
        progressBoard.update(frame, position)
    }

    override fun event(pointer: PointerData): Boolean = progressBoard.event(pointer) || track.event(pointer)

    override fun RhymeDrawScope.draw() {
        lyricsBoard.apply { draw() }
        scoreBoard.apply { draw() }
        comboBoard.apply { draw() }
        track.apply { draw() }
        progressBoard.apply { draw() }
    }
}

// 游戏舞台
@Stable
internal class RhymeStage {
    private var frame: Int = 0
    private var scene: Scene? = null
    private var pointers: MutableLongObjectMap<PointerData>? = null

    fun onInitialize(lyrics: RhymeLyricsConfig, record: ImageBitmap, speed: Int) {
        scene = Scene(lyrics, record)
        pointers = mutableLongObjectMapOf()
    }

    fun onClear() {
        frame = 0
        scene = null
        pointers = null
    }

    fun onUpdate(position: Long) {
        ++frame
        scene?.update(frame, position)
    }

    private fun onEvent(pointer: PointerData) {
        scene?.event(pointer)
    }

    suspend fun PointerInputScope.detectPointer(scale: Float) {
        pointers?.let { pointerMap ->
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    for (change in event.changes) {
                        val id = change.id.value
                        val position = change.position / scale
                        when {
                            change.changedToDown() -> {
                                PointerData(id, position).let { data ->
                                    pointerMap[id] = data
                                    onEvent(data)
                                }
                            }
                            change.changedToUp() -> {
                                pointerMap[id]?.let { data ->
                                    data.end = position
                                    onEvent(data)
                                    pointerMap -= id
                                }
                            }
                            else -> {
                                pointerMap[id]?.let { data ->
                                    data.move = position
                                    onEvent(data)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun RhymeDrawScope.onDraw() {
        scene?.apply { draw() }
    }

    fun onResult(): RhymeResult {
        return RhymeResult(0)
    }
}