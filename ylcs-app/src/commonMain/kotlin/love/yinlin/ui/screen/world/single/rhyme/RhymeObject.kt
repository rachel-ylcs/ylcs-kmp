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

    fun DrawScope.image(image: ImageBitmap, position: Offset = Offset.Zero, size: Size = this@RhymeObject.size) =
        this.drawImage(image = image, dstOffset = position.roundToIntOffset(), dstSize = size.roundToIntSize(), filterQuality = FilterQuality.High)

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
    center: Offset,
    private val duration: Long,
    private val record: ImageBitmap
) : RhymeDynamic(), RhymeContainer.Circle, RhymeEvent {
    companion object {
        const val STROKE = 8f
        const val RADIUS = 64f
    }

    override val position: Offset = center.translate(-RADIUS, -RADIUS)
    override val size: Size = Size(RADIUS * 2, RADIUS * 2)

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
        private val trackCenter: Offset,
        private val scoreBoard: ScoreBoard,
        private val comboBoard: ComboBoard
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

        private val current: Int? by mutableStateOf(null)

        override fun onUpdate(position: Long) {

        }

        override fun onEvent(pointer: Pointer): Boolean {
            if (pointer.up) {
                val score = comboBoard.updateAction(when (Random.nextInt(1, 4)) {
                    1 -> ComboBoard.Action.PERFECT
                    2 -> ComboBoard.Action.GOOD
                    else -> ComboBoard.Action.MISS
                })
                scoreBoard.addScore(score)
            }
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
            // 画轨道射线
            for (pos in Tracks) drawTrackLine(start = trackCenter, end = pos, stroke = TRACK_STROKE)
        }
    }

    // 音符队列
    @Stable
    private class NoteQueue(
        private val trackCenter: Offset,
        lyrics: RhymeLyricsConfig
    ) : RhymeDynamic(), RhymeContainer.Rectangle {
        companion object {
            const val PERFECT_RATIO = 1.25f
            const val GOOD_RATIO = 2f
            const val MISS_RATIO = 4f

            const val NOTE_PADDING_RATIO = 0.9f
        }

        override val position: Offset = Offset.Zero
        override val size: Size = Size.Game

        //                                                  音符生命周期
        // ------------------------------------------------------------------------------------------------------------
        //    ↑          ↑             ↑              ↑                ↑              ↑             ↑             ↑
        //   出现  可点击开始(MISS)  好开始(GOOD)  完美开始(PERFECT)  完美结束(PERFECT)  好结束(GOOD)  可点击结束(MISS)    消失
        //                                                发声    收声
        //                                                [ 提示区域 ]
        // 字符的发声点和收声点的间隔恰好是经过提示区域, 中点对应轨道线 TIP_AREA_START 和 TIP_AREA_END 的中点
        // 提示区域的PERFECT_RATIO倍邻域为完美
        // 提示区域的GOOD_RATIO倍邻域为好
        // 提示区域的MISS_RATIO倍邻域为错过
        // 再往外的邻域不响应点击
        @Stable
        data class DynamicAction(
            val action: RhymeAction, // 音符操作
            val appearance: Long, // 出现刻
            val missStartOffset: Int, // MISS 起始偏移
            val goodStartOffset: Int, // GOOD 起始偏移
            val perfectStartOffset: Int, // PERFECT 起始偏移
            val perfectEndOffset: Int, // PERFECT 结束偏移
            val goodEndOffset: Int, // GOOD 结束偏移
            val missEndOffset: Int, // MISS 结束偏移
            val dismissOffset: Int, // 消失偏移
            val id: Int, // ID (优化状态比较)
        ) {
            override fun equals(other: Any?): Boolean = (other as? DynamicAction)?.id == this.id
            override fun hashCode(): Int = id.hashCode()
        }

        @Stable
        data class DynamicActionWrapper(
            val action: DynamicAction,
            val offset: Int = 0, // 当前偏移
        )

        private val textCache = TextCache()

        private val lock = SynchronizedObject()
        // 双指针维护双端队列
        private var pushIndex by mutableIntStateOf(0)
        private var popIndex by mutableIntStateOf(0)
        // 预编译列表
        private val queue = buildList {
            val baseRatio = (TipArea.TIP_AREA_END - TipArea.TIP_AREA_START) / 2
            val centerRatio = (TipArea.TIP_AREA_END + TipArea.TIP_AREA_START) / 2
            var id = 0
            lyrics.lyrics.fastForEach { line ->
                val theme = line.theme
                for (i in theme.indices) {
                    val action = theme[i]
                    val start = (theme.getOrNull(i - 1)?.end ?: 0) + line.start
                    val end = action.end + line.start
                    val duration = ((end - start) / (TipArea.TIP_AREA_END - TipArea.TIP_AREA_START)).toInt()
                    val appearance = (start - duration * TipArea.TIP_AREA_START).toLong()
                    add(DynamicActionWrapper(DynamicAction(
                        action = action,
                        appearance = appearance,
                        missStartOffset = ((centerRatio - baseRatio * MISS_RATIO) * duration).toInt().coerceIn(0, duration),
                        goodStartOffset = ((centerRatio - baseRatio * GOOD_RATIO) * duration).toInt().coerceIn(0, duration),
                        perfectStartOffset = ((centerRatio - baseRatio * PERFECT_RATIO) * duration).toInt().coerceIn(0, duration),
                        perfectEndOffset = ((centerRatio + baseRatio * PERFECT_RATIO) * duration).toInt().coerceIn(0, duration),
                        goodEndOffset = ((centerRatio + baseRatio * GOOD_RATIO) * duration).toInt().coerceIn(0, duration),
                        missEndOffset = ((centerRatio + baseRatio * MISS_RATIO) * duration).toInt().coerceIn(0, duration),
                        dismissOffset = duration,
                        id = id++
                    )))
                }
            }
        }.toMutableStateList()

        override fun onUpdate(position: Long) {
            synchronized(lock) {
                val index1 = popIndex
                val index2 = pushIndex
                // 音符消失
                queue.getOrNull(index1)?.let { (action, _) ->
                    // 到达消失刻
                    if (position >= action.appearance + action.dismissOffset) ++popIndex
                }
                // 音符出现
                queue.getOrNull(index2)?.let { (action, _) ->
                    // 到达出现刻
                    if (position >= action.appearance) ++pushIndex
                }
                // 更新音符进度
                for (i in index1 ..< index2) {
                    val action = queue[i].action
                    queue[i] = DynamicActionWrapper(action = action, offset = (position - action.appearance).toInt().coerceIn(0, action.dismissOffset))
                }
            }
        }

        private fun DrawScope.drawNoteAction(textManager: RhymeTextManager, action: RhymeAction.Note, progress: Float) {
            // 计算轨道索引与等级
            val base = action.scale - 1
            val trackIndex = base % 7 + 1
            val trackLevel = base / 7
            // 计算轨道两条射线末端坐标
            val trackLeft = Track.Tracks[(trackIndex + 1) % 7]
            val trackRight = Track.Tracks[((trackIndex + 2) % 7).let { if (it == 0) 7 else it }]
            // 计算进度所在矩形顶点
            val path = Path(arrayOf(
                trackCenter.onLine(trackLeft, progress),
                trackCenter.onLine(trackRight, progress),
                trackCenter.onLine(trackRight, progress + TipArea.TIP_AREA_END - TipArea.TIP_AREA_START),
                trackCenter.onLine(trackLeft, progress + TipArea.TIP_AREA_END - TipArea.TIP_AREA_START)
            ))
            // 计算矩形中心坐标并缩放边距
            val rc = trackCenter.onLine(trackLeft.onCenter(trackRight), progress + (TipArea.TIP_AREA_END - TipArea.TIP_AREA_START) / 2)
            scale(scale = NOTE_PADDING_RATIO, pivot = rc) {
                path(
                    color = when (trackLevel) {
                        1 -> Colors.Purple2
                        2 -> Colors.Yellow2
                        else -> Colors.Red2
                    },
                    path = path
                )
            }
            // 文字测试
            // TODO: 设计有问题, 时长较长的字即使先出现仍然会被后出现但时长较短的字超过, 导致局面混乱
            val content = textCache.measureText(textManager, action.ch, 50f)
            textManager.run { text(content, trackCenter.onLine(trackLeft, progress), Colors.White) }
        }

        private fun DrawScope.drawSlurAction(action: RhymeAction.Slur, progress: Float) {

        }

        override fun DrawScope.onDraw(textManager: RhymeTextManager) {
            // 遍历已显示的音符队列
            for (i in popIndex ..< pushIndex) {
                val (action, offset) = queue.getOrNull(i) ?: break
                // 计算进度百分比
                val progress = (offset / action.dismissOffset.toFloat())
                if (progress <= 0f || progress >= 1f) continue
                when (val actualAction = action.action) {
                    is RhymeAction.Note -> drawNoteAction(textManager, actualAction, progress)
                    is RhymeAction.Slur -> drawSlurAction(actualAction, progress)
                }
            }
        }
    }

    override val position: Offset = Offset.Zero
    override val size: Size = Size.Game

    private val tipArea = TipArea(center)
    private val track = Track(center, scoreBoard, comboBoard)
    private val noteQueue = NoteQueue(center, lyrics)

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
private class ScoreBoard : RhymeDynamic(), RhymeContainer.Rectangle {
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
        rotateRad(atan(288 / 768f), Offset.Zero)
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
private class ComboBoard : RhymeDynamic(), RhymeContainer.Rectangle {
    @Stable
    enum class Action(val score: Int, val title: String, val brush: Brush) {
        MISS(0, "MISS", Brush.verticalGradient(listOf(Colors.Ghost, Colors.Pink4))),
        GOOD(1, "GOOD", Brush.verticalGradient(listOf(Colors.Gray2, Colors.Orange2))),
        PERFECT(2, "PERFECT", Brush.verticalGradient(listOf(Colors.Yellow2, Colors.Green2)))
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
        rotateRad(atan(-288 / 768f), Offset(textWidth, textHeight))
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
        var score = newAction.score
        if (newAction == Action.MISS) combo = 0 // 清空连击
        else score += ++combo / COMBO_COUNT // 连击得分奖励
        return score
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
    record: ImageBitmap
) : RhymeDynamic(), RhymeContainer.Rectangle, RhymeEvent {
    override val position: Offset = Offset.Zero
    override val size: Size = Size.Game

    private val center: Offset = Offset(size.width / 2, 360f)

    val lyricsBoard = LyricsBoard(lyrics)
    val scoreBoard = ScoreBoard()
    val comboBoard = ComboBoard()
    val noteBoard = NoteBoard(lyrics, center, scoreBoard, comboBoard)
    val progressBoard = ProgressBoard(center, lyrics.duration, record)

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

    fun onInitialize(lyrics: RhymeLyricsConfig, record: ImageBitmap) {
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