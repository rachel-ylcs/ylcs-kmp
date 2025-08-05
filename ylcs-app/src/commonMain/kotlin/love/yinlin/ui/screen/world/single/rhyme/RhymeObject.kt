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
import love.yinlin.data.music.RhymeAction
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.extension.*
import kotlin.math.*

private val Size.Companion.Game get() = Size(1920f, 1080f)

// 指针数据
internal data class Pointer(
    val startPosition: Offset,
    val startTime: Long,
    var position: Offset? = null,
    var time: Long? = null,
    var up: Boolean = false,
)

private fun Path(positions: Array<Offset>): Path = Path().apply {
    positions.firstOrNull()?.let { first ->
        moveTo(first.x, first.y)
        for (i in 1 ..< positions.size) {
            val position = positions[i]
            lineTo(position.x, position.y)
        }
        close()
    }
}

// 图片资源集
@Stable
internal data class ImageSet(
    val record: ImageBitmap,
    val noteLayoutMap: ImageBitmap
)

// 文本绘制缓存
@Stable
internal class TextCache(maxSize: Int = 8) {
    @Stable
    private data class CacheKey(
        val text: String,
        val height: Float,
        val fontWeight: FontWeight
    )

    private val lruCache = lruCache<CacheKey, Paragraph>(maxSize)

    fun measureText(manager: RhymeTextManager, text: String, height: Float, fontWeight: FontWeight = FontWeight.Light): Paragraph {
        val cacheKey = CacheKey(text, height, fontWeight)
        val cacheResult = lruCache[cacheKey]
        if (cacheResult != null) return cacheResult
        val newResult = manager.makeParagraph(text, height, fontWeight)
        lruCache.put(cacheKey, newResult)
        return newResult
    }
}

// 文本绘制管理器
@Stable
internal class RhymeTextManager(
    font: Font,
    private val fontFamilyResolver: FontFamily.Resolver
) {
    private val fontFamily = FontFamily(font)
    private val density = Density(1f)

    fun makeParagraph(text: String, height: Float, fontWeight: FontWeight = FontWeight.Light): Paragraph {
        // 查询缓存
        val intrinsics = ParagraphIntrinsics(
            text = text,
            style = TextStyle(
                fontSize = TextUnit(height / 1.17f, TextUnitType.Sp),
                fontWeight = fontWeight,
                fontFamily = fontFamily
            ),
            annotations = emptyList(),
            density = density,
            fontFamilyResolver = fontFamilyResolver,
            placeholders = emptyList()
        )
        return Paragraph(
            paragraphIntrinsics = intrinsics,
            constraints = Constraints.fitPrioritizingWidth(minWidth = 0, maxWidth = intrinsics.maxIntrinsicWidth.toInt(), minHeight = 0, maxHeight = height.toInt()),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
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

    fun DrawScope.text(
        content: Paragraph,
        position: Offset,
        brush: Brush,
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
                brush = brush,
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

    fun DrawScope.circle(color: Color, position: Offset = center, radius: Float = max(size.width, size.height) / 2, alpha: Float = 1f, style: DrawStyle = Fill) =
        this.drawCircle(color = color, radius = radius, center = position, alpha = alpha, style = style)

    fun DrawScope.circle(brush: Brush, position: Offset = center, radius: Float = max(size.width, size.height) / 2, alpha: Float = 1f, style: DrawStyle = Fill) =
        this.drawCircle(brush = brush, radius = radius, center = position, alpha = alpha, style = style)

    fun DrawScope.rect(color: Color, position: Offset = Offset.Zero, size: Size = this@RhymeObject.size, alpha: Float = 1f, style: DrawStyle = Fill) =
        this.drawRect(color = color, topLeft = position, size = size, alpha = alpha, style = style)

    fun DrawScope.rect(brush: Brush, position: Offset = Offset.Zero, size: Size = this@RhymeObject.size, alpha: Float = 1f, style: DrawStyle = Fill) =
        this.drawRect(brush = brush, topLeft = position, size = size, alpha = alpha, style = style)

    fun DrawScope.path(color: Color, path: Path, alpha: Float = 1f, style: DrawStyle = Fill) =
        this.drawPath(path = path, color = color, alpha = alpha, style = style)

    fun DrawScope.path(brush: Brush, path: Path, alpha: Float = 1f, style: DrawStyle = Fill) =
        this.drawPath(path = path, brush = brush, alpha = alpha, style = style)

    fun DrawScope.quadrilateral(color: Color, area: Array<Offset>, alpha: Float = 1f, style: DrawStyle = Fill) =
        this.drawPath(path = Path(area), color = color, alpha = alpha, style = style)

    fun DrawScope.quadrilateral(brush: Brush, area: Array<Offset>, alpha: Float = 1f, style: DrawStyle = Fill) =
        this.drawPath(path = Path(area), brush = brush, alpha = alpha, style = style)

    fun DrawScope.roundRect(color: Color, radius: Float, position: Offset = Offset.Zero, size: Size = this@RhymeObject.size, alpha: Float = 1f) =
        this.drawRoundRect(color = color, topLeft = position, size = size, cornerRadius = CornerRadius(radius, radius), alpha = alpha)

    fun DrawScope.roundRect(brush: Brush, radius: Float, position: Offset = Offset.Zero, size: Size = this@RhymeObject.size, alpha: Float = 1f) =
        this.drawRoundRect(brush = brush, topLeft = position, size = size, cornerRadius = CornerRadius(radius, radius), alpha = alpha)

    fun DrawScope.arc(color: Color, startAngle: Float, sweepAngle: Float, position: Offset = Offset.Zero, size: Size = this@RhymeObject.size, alpha: Float = 1f, style: DrawStyle = Fill) =
        this.drawArc(color = color, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, topLeft = position, size = size, alpha = alpha, style = style)

    fun DrawScope.arc(brush: Brush, startAngle: Float, sweepAngle: Float, position: Offset = Offset.Zero, size: Size = this@RhymeObject.size, alpha: Float = 1f, style: DrawStyle = Fill) =
        this.drawArc(brush = brush, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, topLeft = position, size = size, alpha = alpha, style = style)

    fun DrawScope.image(image: ImageBitmap, position: Offset = Offset.Zero, size: Size = this@RhymeObject.size, alpha: Float = 1f) =
        this.drawImage(
            image = image,
            dstOffset = position.roundToIntOffset(),
            dstSize = size.roundToIntSize(),
            alpha = alpha,
            filterQuality = FilterQuality.High
        )

    fun DrawScope.image(image: ImageBitmap, srcPosition: Offset, srcSize: Size, dstPosition: Offset, dstSize: Size, alpha: Float = 1f) =
        this.drawImage(
            image = image,
            srcOffset = srcPosition.roundToIntOffset(),
            srcSize = srcSize.roundToIntSize(),
            dstOffset = dstPosition.roundToIntOffset(),
            dstSize = dstSize.roundToIntSize(),
            alpha = alpha,
            filterQuality = FilterQuality.High
        )

    fun DrawScope.circleImage(image: ImageBitmap, position: Offset = Offset.Zero, size: Size = this@RhymeObject.size) =
        this.clipPath(Path().apply { addOval(Rect(position, size)) }) { this.image(image = image, position = position, size = size) }

    inline fun DrawScope.clip(position: Offset = Offset.Zero, size: Size = this@RhymeObject.size, block: DrawScope.() -> Unit) =
        this.clipRect(left = position.x, top = position.y, right = (position.x + size.width), bottom = (position.y + size.height), block = block)

    inline fun DrawScope.clip(path: Path, block: DrawScope.() -> Unit) =
        this.clipPath(path, block = block)
}

// 动态实体
@Stable
private abstract class RhymeDynamic : RhymeObject() {
    abstract fun onUpdate(position: Long)
}

// 进度板
@Stable
private class ProgressBoard(
    imageSet: ImageSet,
    center: Offset,
    private val duration: Long
) : RhymeDynamic(), RhymeContainer.Circle, RhymeEvent {
    companion object {
        const val STROKE = 8f
        const val RADIUS = 64f
    }

    override val position: Offset = center.translate(-RADIUS, -RADIUS)
    override val size: Size = Size(RADIUS * 2, RADIUS * 2)

    private val record = imageSet.record
    // 封面旋转角
    private var angle: Float by mutableFloatStateOf(0f)
    // 游戏进度
    private var progress: Float by mutableFloatStateOf(0f)

    override fun onUpdate(position: Long) {
        progress = if (duration == 0L) 0f else (position / duration.toFloat()).coerceIn(0f, 1f)
        angle += 20 / RhymeConfig.FPS.toFloat()
    }

    override fun onEvent(pointer: Pointer): Boolean = pointer.up && pointer.startPosition in this

    override fun DrawScope.onDraw(textManager: RhymeTextManager) {
        // 画封面
        rotate(angle, Offset(RADIUS, RADIUS)) { circleImage(record) }
        // 画时长
        arc(Colors.White, -90f, 360f, style = Stroke(width = STROKE, cap = StrokeCap.Round))
        // 画进度
        arc(Colors.Green4, -90f, 360f * progress, style = Stroke(width = STROKE, cap = StrokeCap.Round))
    }
}

// 音符板
@Stable
private class NoteBoard(
    lyrics: RhymeLyricsConfig,
    imageSet: ImageSet,
    center: Offset,
    scoreBoard: ScoreBoard,
    comboBoard: ComboBoard
) : RhymeDynamic(), RhymeContainer.Rectangle, RhymeEvent {
    // 提示区域
    @Stable
    private class TipArea(
        trackCenter: Offset
    ) : RhymeObject(), RhymeContainer.Rectangle {
        @Stable
        private class Area(pos1: Offset, pos2: Offset, pos3: Offset, pos4: Offset) {
            val path: Path = Path(arrayOf(pos1, pos2, pos3, pos4))
            private val colorStops1 = arrayOf(
                0f to Colors.Steel3.copy(alpha = 0.8f),
                0.05f to Colors.Steel3.copy(alpha = 0.4f),
                0.1f to Colors.Steel3.copy(alpha = 0.1f),
                0.2f to Colors.Steel3.copy(alpha = 0.02f),
                1f to Colors.Transparent
            )
            // colorStops2 = 3 * colorStops1
            private val colorStops2 = arrayOf(
                0f to Colors.Steel3.copy(alpha = 0.8f),
                0.15f to Colors.Steel3.copy(alpha = 0.4f),
                0.3f to Colors.Steel3.copy(alpha = 0.1f),
                0.6f to Colors.Steel3.copy(alpha = 0.02f),
                1f to Colors.Transparent
            )
            private val isVertical = (pos3.y - pos1.y) > (pos3.x - pos1.x)
            private val startX = min(pos1.x, pos4.x)
            private val endX = max(pos2.x, pos3.x)
            private val startY = min(pos1.y, pos2.y)
            private val endY = max(pos3.y, pos4.y)
            private val horizontalBrush = if (isVertical) colorStops2 else colorStops1
            private val verticalBrush = if (isVertical) colorStops1 else colorStops2
            val brush1: Brush = Brush.horizontalGradient(*horizontalBrush, startX = startX, endX = endX)
            val brush2: Brush = Brush.horizontalGradient(*horizontalBrush, startX = endX, endX = startX)
            val brush3: Brush = Brush.verticalGradient(*verticalBrush, startY = startY, endY = endY)
            val brush4: Brush = Brush.verticalGradient(*verticalBrush, startY = endY, endY = startY)
        }

        companion object {
            const val TIP_AREA_START = 0.8f
            const val TIP_AREA_END = 0.9f
            const val TIP_AREA_RANGE = TIP_AREA_END - TIP_AREA_START
            const val TIP_AREA_STROKE = 5f
        }

        override val position: Offset = Offset.Zero
        override val size: Size = Size.Game

        private val points = arrayOf(
            trackCenter.onLine(Offset(0f, 0f), TIP_AREA_START),
            trackCenter.onLine(Offset(0f, 0f), TIP_AREA_END),
            trackCenter.onLine(Offset(0f, size.height / 2), TIP_AREA_END),
            trackCenter.onLine(Offset(0f, size.height / 2), TIP_AREA_START),
            trackCenter.onLine(Offset(0f, size.height), TIP_AREA_START),
            trackCenter.onLine(Offset(0f, size.height), TIP_AREA_END),
            trackCenter.onLine(Offset(size.width / 3, size.height), TIP_AREA_END),
            trackCenter.onLine(Offset(size.width / 3, size.height), TIP_AREA_START),
            trackCenter.onLine(Offset(size.width * 2 / 3, size.height), TIP_AREA_START),
            trackCenter.onLine(Offset(size.width * 2 / 3, size.height), TIP_AREA_END),
            trackCenter.onLine(Offset(size.width, size.height), TIP_AREA_END),
            trackCenter.onLine(Offset(size.width, size.height), TIP_AREA_START),
            trackCenter.onLine(Offset(size.width, size.height / 2), TIP_AREA_START),
            trackCenter.onLine(Offset(size.width, size.height / 2), TIP_AREA_END),
            trackCenter.onLine(Offset(size.width, 0f), TIP_AREA_END),
            trackCenter.onLine(Offset(size.width, 0f), TIP_AREA_START),
        )
        private val areas = arrayOf(
            Area(points[1], points[0], points[3], points[2]),
            Area(points[2], points[3], points[4], points[5]),
            Area(points[4], points[7], points[6], points[5]),
            Area(points[7], points[8], points[9], points[6]),
            Area(points[8], points[11], points[10], points[9]),
            Area(points[12], points[13], points[10], points[11]),
            Area(points[15], points[14], points[13], points[12]),
        )

        override fun DrawScope.onDraw(textManager: RhymeTextManager) {
            for (area in areas) {
                // 画提示区域线
                path(Colors.White, area.path, style = Stroke(width = TIP_AREA_STROKE))
                // 提示区域内发光遮罩
                path(area.brush1, area.path)
                path(area.brush2, area.path)
                path(area.brush3, area.path)
                path(area.brush4, area.path)
            }
        }
    }

    // 轨道
    @Stable
    private class Track(
        private val scoreBoard: ScoreBoard,
        private val comboBoard: ComboBoard,
        private val trackCenter: Offset
    ) : RhymeDynamic(), RhymeContainer.Rectangle, RhymeEvent {
        companion object {
            const val TRACK_STROKE = 20f
            val Tracks = arrayOf(
                Offset(0f, 0f),
                Offset(0f, Size.Game.height / 2),
                Offset(0f, Size.Game.height),
                Offset(Size.Game.width / 3, Size.Game.height),
                Offset(Size.Game.width * 2 / 3, Size.Game.height),
                Offset(Size.Game.width, Size.Game.height),
                Offset(Size.Game.width, Size.Game.height / 2),
                Offset(Size.Game.width, 0f),
            )
        }

        override val position: Offset = Offset.Zero
        override val size: Size = Size.Game

        private var currentTrack: Int? by mutableStateOf(null)

        override fun onUpdate(position: Long) {

        }

        private val trackSlopes = Tracks.map { trackCenter.slope(it) }
        private val trackShapes = List(7) { Path(arrayOf(Tracks[it], Tracks[it + 1], trackCenter)) }

        private fun calcTrack(pos: Offset): Int? {
            val slope = trackCenter.slope(pos)
            return if (pos.x > Size.Game.width / 2) {
                if (slope >= trackSlopes[4]) 3
                else if (slope >= trackSlopes[5]) 4
                else if (slope >= trackSlopes[6]) 5
                else if (slope >= trackSlopes[7]) 6
                else null
            }
            else {
                if (slope >= trackSlopes[0]) null
                else if (slope >= trackSlopes[1]) 0
                else if (slope >= trackSlopes[2]) 1
                else if (slope >= trackSlopes[3]) 2
                else 3
            }
        }

        override fun onEvent(pointer: Pointer): Boolean {
            // 检查是否在范围内
            val pos = pointer.position
            if (pos == null) {
                // 按下
                calcTrack(pointer.startPosition)?.let { track ->
                    currentTrack = track
                }
            }
            else {
                calcTrack(pos)?.let { track ->
                    if (pointer.up) currentTrack = null // 抬起
                    else { // 移动

                    }
                }
            }
//            if (pointer.up) {
//                val score = comboBoard.updateAction(when (Random.nextInt(0, 4)) {
//                    0 -> ComboBoard.Action.PERFECT
//                    1 -> ComboBoard.Action.GOOD
//                    2 -> ComboBoard.Action.BAD
//                    else -> ComboBoard.Action.MISS
//                })
//                scoreBoard.addScore(score)
//            }
            return true
        }

        private fun DrawScope.drawTrackLine(start: Offset, end: Offset, stroke: Float) {
            // 阴影
            repeat(5) {
                line(Colors.Steel3, start, end, Stroke(width = stroke * (1.15f + it * 0.15f), cap = StrokeCap.Round), 0.15f - (it * 0.03f))
            }
            // 光带
            line(Colors.Steel4, start, end, Stroke(width = stroke, cap = StrokeCap.Round), 0.7f)
            // 高光
            line(Colors.White, start, end, Stroke(width = stroke * 0.8f, cap = StrokeCap.Round), 0.8f)
        }

        override fun DrawScope.onDraw(textManager: RhymeTextManager) {
            // 画当前按下轨道高光
            currentTrack?.let {
                path(
                    Colors.Steel3.copy(alpha = 0.2f),
                    path = trackShapes[it],
                    style = Fill
                )
            }
            // 画轨道射线
            for (pos in Tracks) drawTrackLine(start = trackCenter, end = pos, stroke = TRACK_STROKE)
        }
    }

    // 音符队列
    @Stable
    private class NoteQueue(
        lyrics: RhymeLyricsConfig,
        imageSet: ImageSet,
        trackCenter: Offset
    ) : RhymeDynamic(), RhymeContainer.Rectangle {
        companion object {
            const val TRACK_DURATION = (150 / TipArea.TIP_AREA_RANGE).toLong()
        }

        @Stable
        private sealed interface DynamicAction {
            val action: RhymeAction // 音符操作
            val appearance: Long // 出现刻
            val startTrack: Int // 起始轨道
            val endTrack: Int // 结束轨道

            //                            音符生命周期
            // ------------------------------------------------------------------
            //    ↑     ↑    ↑     ↑      ↑        ↑      ↑     ↑     ↑      ↑
            //   出现  MISS  BAD  GOOD  PERFECT  PERFECT  GOOD  BAD  MISS    消失
            //                               发声
            // 字符的发声点是经过提示区域的开始
            // 提示区域的 PERFECT_RATIO 倍邻域为完美
            // 提示区域的 GOOD_RATIO 倍邻域为好
            // 提示区域的 MISS_RATIO 倍邻域为错过
            // 再往外的邻域不响应点击
            @Stable
            class Note(
                override val action: RhymeAction.Note, // 音符操作
                start: Long, // 发声时间
            ) : DynamicAction {
                companion object {
                    const val PERFECT_RATIO = 0.25f
                    const val GOOD_RATIO = 0.5f
                    const val BAD_RATIO = 1f
                    const val MISS_RATIO = 3f
                }

                override val appearance: Long = start - (TRACK_DURATION * TipArea.TIP_AREA_START).toInt()
                override val startTrack: Int = (action.scale - 1) % 7 + 1
                override val endTrack: Int = startTrack

                val perfect by lazy { makeRange(PERFECT_RATIO) }
                val good by lazy { makeRange(GOOD_RATIO) }
                val bad by lazy { makeRange(BAD_RATIO) }
                val miss by lazy { makeRange(MISS_RATIO) }

                private fun makeRange(result: Float): LongRange {
                    val ratio = TipArea.TIP_AREA_RANGE * result
                    val startOffset = (TRACK_DURATION * (TipArea.TIP_AREA_END - ratio)).toLong().coerceAtLeast(0L)
                    val endOffset = (TRACK_DURATION * (TipArea.TIP_AREA_END + ratio)).toLong().coerceAtMost(TRACK_DURATION)
                    return (appearance + startOffset) .. (appearance + endOffset)
                }
            }

            @Stable
            class Slur(
                override val action: RhymeAction.Slur, // 音符操作
                start: Long, // 发声时间
                end: Long, // 收声时间
            ) : DynamicAction {
                override val appearance: Long = start
                override val startTrack: Int = (action.scale.first() - 1) % 7 + 1
                override val endTrack: Int = (action.scale.last() - 1) % 7 + 1
            }
        }

        @Stable
        private class ActionImageMap(
            private val noteLayoutMap: ImageBitmap,
            private val trackCenter: Offset
        ) {
            @Stable
            private sealed interface ActionImage {
                val srcSize: Size
                val srcOffsets: List<Offset>
                val dstOffset: Offset
                val dstSize: Size
            }

            @Stable
            class Note(
                srcOffset: Offset,
                override val srcSize: Size,
                override val dstOffset: Offset
            ) : ActionImage {
                override val srcOffsets = List(4) { srcOffset.translate(x = it * srcSize.width) }
                override val dstSize: Size = srcSize
            }

            private val extraNoteWidth = trackCenter.x * TipArea.TIP_AREA_RANGE
            private val extraNoteHeight = (Track.Tracks[2].y - trackCenter.y) * TipArea.TIP_AREA_RANGE
            private val topNoteSize = Size(extraNoteWidth, Track.Tracks[1].y * (1 + TipArea.TIP_AREA_RANGE))
            private val bottomNoteSize = Size(extraNoteWidth, Track.Tracks[1].y + extraNoteHeight)
            private val leftRightNoteSize = Size(extraNoteWidth + Track.Tracks[3].x, extraNoteHeight)
            private val centerNoteSize = Size(Track.Tracks[3].x * (1 + TipArea.TIP_AREA_RANGE), extraNoteHeight)

            private val noteMap = arrayOf(
                Note(
                    srcOffset = Offset(topNoteSize.width * 0, centerNoteSize.height * 3),
                    srcSize = topNoteSize,
                    dstOffset = trackCenter.onLine(Track.Tracks[0], 1 + TipArea.TIP_AREA_RANGE)
                ),
                Note(
                    srcOffset = Offset(topNoteSize.width * 4, centerNoteSize.height * 3),
                    srcSize = bottomNoteSize,
                    dstOffset = Track.Tracks[1].translate(x = -extraNoteWidth)
                ),
                Note(
                    srcOffset = Offset(0f, centerNoteSize.height * 0),
                    srcSize = leftRightNoteSize,
                    dstOffset = Track.Tracks[2].translate(x = -extraNoteWidth)
                ),
                Note(
                    srcOffset = Offset(0f, centerNoteSize.height * 1),
                    srcSize = centerNoteSize,
                    dstOffset = Track.Tracks[3].translate(x = -TipArea.TIP_AREA_RANGE * (trackCenter.x - Track.Tracks[3].x))
                ),
                Note(
                    srcOffset = Offset(0f, centerNoteSize.height * 2),
                    srcSize = leftRightNoteSize,
                    dstOffset = Track.Tracks[4]
                ),
                Note(
                    srcOffset = Offset(topNoteSize.width * 12, centerNoteSize.height * 3),
                    srcSize = bottomNoteSize,
                    dstOffset = Track.Tracks[6]
                ),
                Note(
                    srcOffset = Offset(topNoteSize.width * 8, centerNoteSize.height * 3),
                    srcSize = topNoteSize,
                    dstOffset = Track.Tracks[7].translate(y = -TipArea.TIP_AREA_RANGE * trackCenter.y)
                )
            )

            fun DrawScope.drawNoteAction(obj: RhymeObject, trackIndex: Int, trackLevel: Int, progress: Float) {
                noteMap.getOrNull(trackIndex)?.let { actionImage ->
                    obj.run {
                        image(
                            image = noteLayoutMap,
                            srcPosition = actionImage.srcOffsets[trackLevel],
                            srcSize = actionImage.srcSize,
                            dstPosition = trackCenter.onLine(actionImage.dstOffset, progress),
                            dstSize = actionImage.dstSize * progress
                        )
                    }
                }
            }
        }

        override val position: Offset = Offset.Zero
        override val size: Size = Size.Game

        private val actionImageMap = ActionImageMap(imageSet.noteLayoutMap, trackCenter)

        private val lock = SynchronizedObject()
        // 双指针维护基列表
        private var pushIndex = 0
        private var popIndex = 0
        // 预编译列表
        private val prebuildList = buildList {
            lyrics.lyrics.fastForEach { line ->
                val theme = line.theme
                for (i in theme.indices) {
                    val action = theme[i]
                    val start = (theme.getOrNull(i - 1)?.end ?: 0) + line.start
                    val end = action.end + line.start
                    add(when (val action = theme[i]) {
                        is RhymeAction.Note -> DynamicAction.Note(action = action, start = start)
                        is RhymeAction.Slur -> DynamicAction.Slur(action = action, start = start, end = end)
                    })
                }
            }
        }
        // 在场音符集
        private val actions = Array(7) { mutableStateSetOf<DynamicAction>() }
        // 当前刻
        private var current by mutableLongStateOf(0)

        override fun onUpdate(position: Long) {
            synchronized(lock) {
                val index1 = popIndex
                val index2 = pushIndex
                // 音符消失
                prebuildList.getOrNull(index1)?.let { action ->
                    // 到达消失刻
                    if (position >= action.appearance + TRACK_DURATION) {
                        ++popIndex
                        actions.getOrNull(action.endTrack)?.remove(action)
                    }
                }
                // 音符出现
                prebuildList.getOrNull(index2)?.let { action ->
                    // 到达出现刻
                    if (position >= action.appearance) {
                        ++pushIndex
                        actions.getOrNull(action.startTrack)?.add(action)
                    }
                }
                // 更新音符进度
                current = position
            }
        }

        private fun DrawScope.drawNoteAction(action: RhymeAction.Note, progress: Float) {
            // 计算轨道索引与等级
            val base = action.scale - 1
            val trackScale = base % 7 + 1
            val trackLevel = base / 7 + 1
            val trackIndex = (trackScale + 1) % 7
            actionImageMap.run { drawNoteAction(this@NoteQueue, trackIndex, trackLevel, progress) }
        }

        private fun DrawScope.drawSlurAction(action: RhymeAction.Slur, progress: Float) {

        }

        override fun DrawScope.onDraw(textManager: RhymeTextManager) {
            // 遍历已显示的音符队列
            for (actionSet in actions) {
                for (action in actionSet) {
                    // 计算进度百分比
                    val progress = (current - action.appearance) / TRACK_DURATION.toFloat()
                    if (progress <= 0f || progress >= 1f) continue
                    when (val actualAction = action.action) {
                        is RhymeAction.Note -> drawNoteAction(actualAction, progress)
                        is RhymeAction.Slur -> drawSlurAction(actualAction, progress)
                    }
                }
            }
        }
    }

    override val position: Offset = Offset.Zero
    override val size: Size = Size.Game

    private val tipArea = TipArea(center)
    private val track = Track(scoreBoard, comboBoard, center)
    private val noteQueue = NoteQueue(lyrics, imageSet, center)

    override fun onUpdate(position: Long) {
        track.run { onUpdate(position) }
        noteQueue.run { onUpdate(position) }
    }

    override fun onEvent(pointer: Pointer): Boolean = track.run { onEvent(pointer) }

    override fun DrawScope.onDraw(textManager: RhymeTextManager) {
        tipArea.run { draw(textManager) }
        track.run { draw(textManager) }
        noteQueue.run { draw(textManager) }
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

    private val textCache = TextCache(16)

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
        val content = textCache.measureText(textManager, line, textHeight)
        val textWidth = content.width
        val start = Offset((this@LyricsBoard.size.width - textWidth) / 2, 0f)
        textManager.run {
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
private class ScoreBoard(
    rotate: Float
) : RhymeDynamic(), RhymeContainer.Rectangle {
    @Stable
    private class ScoreNumber(pos: Offset) : RhymeDynamic(), RhymeContainer.Rectangle {
        // 七段数码管
        //     1
        // 6 ▎ ━  ▎ 2
        //   ▎ 7  ▎
        //   ▎ ━  ▎
        //   ▎    ▎
        // 5 ▎ ━  ▎ 3
        //     4
        companion object {
            const val RECT_WIDTH = 32f
            const val RECT_HEIGHT = 8f
            const val RECT_RADIUS = RECT_HEIGHT / 2
            const val WIDTH = 2 * RECT_HEIGHT + RECT_WIDTH
            const val HEIGHT = 3 * RECT_HEIGHT + 2 * RECT_WIDTH

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

            val Rects = arrayOf(
                Rect(RECT_HEIGHT, 0f, RECT_HEIGHT + RECT_WIDTH, RECT_HEIGHT),
                Rect(RECT_HEIGHT + RECT_WIDTH, RECT_HEIGHT, RECT_HEIGHT * 2 + RECT_WIDTH, RECT_HEIGHT + RECT_WIDTH),
                Rect(RECT_HEIGHT + RECT_WIDTH, RECT_HEIGHT * 2 + RECT_WIDTH, RECT_HEIGHT * 2 + RECT_WIDTH, RECT_HEIGHT * 2 + RECT_WIDTH * 2),
                Rect(RECT_HEIGHT, RECT_HEIGHT * 2 + RECT_WIDTH * 2, RECT_HEIGHT + RECT_WIDTH, RECT_HEIGHT * 3 + RECT_WIDTH * 2),
                Rect(0f, RECT_HEIGHT * 2 + RECT_WIDTH, RECT_HEIGHT, RECT_HEIGHT * 2 + RECT_WIDTH * 2),
                Rect(0f, RECT_HEIGHT, RECT_HEIGHT, RECT_HEIGHT + RECT_WIDTH),
                Rect(RECT_HEIGHT, RECT_HEIGHT + RECT_WIDTH, RECT_HEIGHT + RECT_WIDTH, RECT_HEIGHT * 2 + RECT_WIDTH)
            )

            private fun fetchNumber(v: Byte): Int {
                for (i in 0 .. 9) {
                    if (NumberArray[i] == v) return i
                }
                return 0
            }

            private fun encode(v1: Byte, v2: Byte = 0, v3: Byte = 127): Int = ((v1.toInt() and 0xff) shl 16) or ((v2.toInt() and 0xff) shl 8) or (v3.toInt() and 0xff)
        }

        override val position: Offset = pos
        override val size: Size = Size(WIDTH, HEIGHT)

        private var data by mutableIntStateOf(encode(NumberArray[0]))

        val current: Byte get() = ((data shr 16) and 0xff).toByte()
        val target: Byte get() = ((data shr 8) and 0xff).toByte()
        val alpha: Byte get() = (data and 0xff).toByte()
        val isPlaying: Boolean get() = target.toInt() != 0
        val score: Int get() = fetchNumber(if (isPlaying) target else current)
        val isZero: Boolean get() = (if (isPlaying) target else current) == NumberArray[0]

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

        override fun DrawScope.onDraw(textManager: RhymeTextManager) {
            val a = (alpha / 127f).coerceIn(0f, 1f)
            val v1 = current.toInt()
            val v2 = target.toInt()
            repeat(Rects.size) {
                val mask = 1 shl it
                val rect = Rects[it]
                val b1 = (v1 and mask) == mask
                val b2 = (v2 and mask) == mask
                if (b1) {
                    if (b2) roundRect(Colors.Red4, RECT_RADIUS, rect.topLeft, rect.size, 1f)
                    else roundRect(Colors.Red4, RECT_RADIUS, rect.topLeft, rect.size, a)
                }
                else if (b2) roundRect(Colors.Red4, RECT_RADIUS, rect.topLeft, rect.size, 1 - a)
            }
        }
    }

    companion object {
        const val GAP = ScoreNumber.RECT_HEIGHT
        const val WIDTH = ScoreNumber.WIDTH * 4 + GAP * 3
    }

    override val position: Offset = Offset(Size.Game.width / 2 - ProgressBoard.RADIUS / 2 - 50 - WIDTH, 100f)
    override val size: Size = Size(WIDTH, ScoreNumber.HEIGHT)
    override val transform: (DrawTransform.() -> Unit) = {
        rotateRad(rotate, Offset.Zero)
    }

    private val numbers = Array(4) {
        ScoreNumber(Offset(it * (ScoreNumber.WIDTH + GAP), 0f))
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
        if (numbers.any { !it.isZero }) {
            for (number in numbers) number.run { draw(textManager) }
        }
    }
}

// 连击板
@Stable
private class ComboBoard(
    rotate: Float
) : RhymeDynamic(), RhymeContainer.Rectangle {
    @Stable
    enum class Action(val score: Int, val title: String, val brush: Brush) {
        MISS(0, "MISS", Brush.verticalGradient(listOf(Colors.Ghost, Colors.Pink4))),
        BAD(1, "BAD", Brush.verticalGradient(listOf(Colors.Gray2, Colors.Red6))),
        GOOD(2, "GOOD", Brush.verticalGradient(listOf(Colors.Gray2, Colors.Orange2))),
        PERFECT(3, "PERFECT", Brush.verticalGradient(listOf(Colors.Yellow2, Colors.Green2)))
    }

    companion object {
        const val FPA = RhymeConfig.FPS * 150 / 1000
        const val COMBO_COUNT = 20
    }

    private val textWidth = 270f
    private val textHeight: Float = 90f
    override val position: Offset = Offset(Size.Game.width / 2 + ProgressBoard.RADIUS / 2 + 50, 100f)
    override val size: Size = Size(textWidth, textHeight)
    override val transform: (DrawTransform.() -> Unit) = {
        rotateRad(rotate, Offset(textWidth, textHeight))
    }

    private var action by mutableStateOf<Action?>(null)
    private var combo by mutableIntStateOf(0)
    private var frame by mutableIntStateOf(0)

    private val actionTextCache = TextCache()
    private val comboTextCache = TextCache(16)

    fun updateAction(newAction: Action): Int {
        // 重置进度
        action = newAction
        frame = 0
        // 计算得分
        if (newAction == Action.MISS || newAction == Action.BAD) combo = 0 // 清空连击
        else ++combo // 增加连击
        return newAction.score + combo / COMBO_COUNT // 连击得分奖励
    }

    override fun onUpdate(position: Long) {
        // Animation: Enter | Wait | Exit
        // Frame:      FPA  | FPA  | FPA
        if (frame == FPA * 3) {
            action = null
            frame = 0
        }
        else ++frame
    }

    override fun DrawScope.onDraw(textManager: RhymeTextManager) {
        action?.let { currentAction ->
            //         { 1 - ((x - FPA) / FPA) ^ 2  , 0       <= x <= FPA
            // f(x) =  { 1                          , FPA     <= x <= 2 * FPA
            //         { ((x - 3 * FPA) / FPA) ^ 2  , 2 * FPA <= x <= 3 * FPA
            val progress = when (val currentFrame = frame) {
                in 0 .. FPA -> 1 - (currentFrame - FPA) * (currentFrame - FPA) / (FPA * FPA).toFloat()
                in 2 * FPA .. 3 * FPA -> (currentFrame - 3 * FPA) * (currentFrame - 3 * FPA) / (FPA * FPA).toFloat()
                else -> 1f
            }.coerceIn(0f, 1f)
            // 结果
            val content = actionTextCache.measureText(textManager, currentAction.title, textHeight, FontWeight.Bold)
            scale(progress, this@ComboBoard.size.center) {
                textManager.run {
                    text(
                        content = content,
                        position = Offset((textWidth - content.width) / 2, 0f),
                        brush = currentAction.brush,
                        shadow = Shadow(Colors.Dark, Offset(2f, 2f), 2f)
                    )
                }
            }
            // 连击
            if (combo > 1) {
                val content = comboTextCache.measureText(textManager, "+$combo", textHeight / 2, FontWeight.Bold)
                val topLeft = Offset(textWidth - content.width, textHeight / 2)
                scale(progress, topLeft.translate(content.width / 2, textHeight / 4)) {
                    textManager.run {
                        text(
                            content = content,
                            position = topLeft,
                            color = Colors.White.copy(alpha = (progress * 2).coerceIn(0f, 1f)),
                            shadow = Shadow(Colors.Dark, Offset(1f, 1f), 1f)
                        )
                    }
                }
            }
        }
    }
}

// 场景
@Stable
private class Scene(
    lyrics: RhymeLyricsConfig,
    imageSet: ImageSet
) : RhymeDynamic(), RhymeContainer.Rectangle, RhymeEvent {
    override val position: Offset = Offset.Zero
    override val size: Size = Size.Game

    private val center: Offset = Offset(size.width / 2, 360f)

    val lyricsBoard = LyricsBoard(lyrics)
    val scoreBoard = ScoreBoard(atan(center.y / center.x))
    val comboBoard = ComboBoard(-atan(center.y / center.x))
    val noteBoard = NoteBoard(lyrics, imageSet, center, scoreBoard, comboBoard)
    val progressBoard = ProgressBoard(imageSet, center, lyrics.duration)

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

    fun onInitialize(lyrics: RhymeLyricsConfig, imageSet: ImageSet) {
        scene = Scene(lyrics, imageSet)
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