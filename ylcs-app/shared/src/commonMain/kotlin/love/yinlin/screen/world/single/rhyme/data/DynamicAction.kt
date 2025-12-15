package love.yinlin.screen.world.single.rhyme.data

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import love.yinlin.compose.Colors
import love.yinlin.compose.Path
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.animation.FrameAnimation
import love.yinlin.compose.graphics.AnimatedWebp
import love.yinlin.compose.graphics.SolidColorFilter
import love.yinlin.compose.onLine
import love.yinlin.compose.translate
import love.yinlin.data.music.RhymeAction
import love.yinlin.screen.world.single.rhyme.RhymeAssets
import love.yinlin.screen.world.single.rhyme.RhymeDifficulty
import love.yinlin.screen.world.single.rhyme.RhymePlayConfig

@Stable
sealed class DynamicAction(
    protected val assets: RhymeAssets,
    protected val playConfig: RhymePlayConfig,
    protected val start: Long,
    protected val end: Long,
) {
    companion object {
        const val PERSPECTIVE_K = 3 // 透视参数
        const val HIT_RATIO = 0.8f // 判定线

        val deadline = ActionResult.BAD.endRange(HIT_RATIO) // 死线

        val BlockSize = Size(256f, 85f)
        fun calcBlockRect(level: Int): Pair<Rect, Rect> {
            val left = Rect(Offset(0f, level * BlockSize.height), BlockSize)
            val center = Rect(Offset(BlockSize.width, level * BlockSize.height), BlockSize)
            return left to center
        }

        fun mapTrackIndex(scale: Byte): Int = Tracks.Scales.indexOf((scale - 1) % 7 + 1)

        fun mapNoteLevel(scale: Byte): Int = (scale - 1) / 7

        @Stable
        val ResultColorFilters = ActionResult.entries.map { SolidColorFilter(it.colors.first()) }

        val NoteColors = arrayOf(
            Colors.Cyan3,
            Colors.Green3,
            Colors.Purple3
        )

        @Stable
        val NoteColorFilters = NoteColors.map { SolidColorFilter(it) }

        val SlurColors = arrayOf(
            Colors.Red5,
            Colors.Orange6,
            Colors.Cyan6
        )

        @Stable
        val SlurColorFilters = SlurColors.map { SolidColorFilter(it) }

        val SlurTailColors = arrayOf(
            listOf(Colors(0xFF453A94), Colors(0xFFF43B47), Colors.Transparent),
            listOf(Colors(0xFFFF4E50), Colors(0xFFF9D423), Colors.Transparent),
            listOf(Colors(0xFF6E45E2), Colors(0xFF88D3CE), Colors.Transparent),
        )

        @Stable
        val SlurTailBrushes = SlurTailColors.map { Brush.verticalGradient(it, 0f, Tracks.VirtualHeight) }
    }

    abstract var bindId: Long? // 绑定指针ID
    abstract val action: RhymeAction // 行为
    abstract val appearance: Long // 出现时刻
    abstract val isDismiss: Boolean // 消失

    abstract fun onAdmission() // 入场
    abstract fun onUpdate(tick: Long, callback: ActionCallback) // 更新
    abstract fun onTrackDown(track: Track, tick: Long, callback: ActionCallback): Boolean // 按下
    abstract fun onTrackUp(track: Track, tick: Long, callback: ActionCallback) // 抬起
    abstract fun onTrackTransfer(oldTrack: Track, newTrack: Track, tick: Long, callback: ActionCallback): Boolean // 变轨
    abstract fun Drawer.onDraw() // 渲染

    protected val blockMap by lazy { assets.blockMap() }
    protected val noteClick by lazy { assets.noteClick() }
    protected val noteDismiss by lazy { assets.noteDismiss() }
    protected val longPress by lazy { assets.longPress() }
    protected val longRelease by lazy { assets.longRelease() }

    protected val soundNoteClick by lazy { assets.soundNoteClick }

    protected val difficulty = playConfig.difficulty
    protected val baseDuration = when (difficulty) {
        RhymeDifficulty.Easy -> 3500L
        RhymeDifficulty.Medium -> 3000L
        RhymeDifficulty.Hard -> 2500L
        RhymeDifficulty.Extra -> 2000L
    }
    protected val baseDurationF = baseDuration.toFloat()
}

internal val Float.asUncheckedActual: Float get() = this / (DynamicAction.PERSPECTIVE_K + this * (1 - DynamicAction.PERSPECTIVE_K))
internal val Float.asActual: Float get() = this.coerceIn(0f, 1f).asUncheckedActual.coerceIn(0f, 1f)
internal val Float.asUncheckedVirtual: Float get() = DynamicAction.PERSPECTIVE_K * this / (1 + this * (DynamicAction.PERSPECTIVE_K - 1))
internal val Float.asVirtual: Float get() = this.coerceIn(0f, 1f).asUncheckedVirtual.coerceIn(0f, 1f)

// 绘制透视音符
internal fun Drawer.noteTransform(ratio: Float, track: Track, block: Drawer.(srcRect: Rect) -> Unit) {
    val (matrix, srcRect, _) = track.notePerspectiveMatrix
    transform({
        // 先将轨道底部的画布以顶点为中心缩放到指定进度上
        scale(ratio, Tracks.Vertices)
        // 然后透视
        transform(matrix)
        // 再根据轨道左右位置决定是否水平翻转
        if (track.isRight) flipX(srcRect.center)
    }) {
        block(srcRect)
    }
}

// 绘制平铺动画
internal fun Drawer.drawPlainAnimation(
    track: Track,
    progress: Float,
    asset: AnimatedWebp,
    animation: FrameAnimation,
    scaleRatio: Float = 1f,
    alpha: Float = 1f,
    colorFilter: ColorFilter? = null
) {
    val plainRect = track.plainRect(progress, asset.width.toFloat() / asset.height, scaleRatio)
    drawAnimatedWebp(asset, animation.frame, plainRect, alpha, colorFilter)
}

// 绘制透视动画
internal fun Drawer.drawPerspectiveAnimation(
    track: Track,
    srcRect: Rect,
    asset: AnimatedWebp,
    animation: FrameAnimation,
    alpha: Float = 1f,
    colorFilter: ColorFilter? = null
) {
    val frame = animation.frame.let {
        if (track.isCenter) it + animation.total else it
    }
    drawAnimatedWebp(asset, frame, srcRect, alpha, colorFilter)
}

// 绘制拖尾
internal fun Drawer.drawTrailing(
    track: Track,
    noteLevel: Int,
    headProgress: Float,
    tailProgress: Float,
    alpha: Float = 1f,
    lastResult: Pair<Offset, Offset>? = null
): Pair<Offset, Offset>? {
    if (headProgress <= tailProgress || alpha <= 0f) return null

    // 绘制拖尾
    val headLeft = Tracks.Vertices.onLine(track.bottomTailLeft, headProgress)
    val headRight = Tracks.Vertices.onLine(track.bottomTailRight, headProgress)
    val tailLeft = Tracks.Vertices.onLine(track.bottomTailLeft, tailProgress)
    val tailRight = Tracks.Vertices.onLine(track.bottomTailRight, tailProgress)
    path(
        brush = DynamicAction.SlurTailBrushes[noteLevel],
        path = Path(arrayOf(headLeft, tailLeft, tailRight, headRight)),
        alpha = alpha
    )

    // 绘制终端 (小进度0.005f需要先转换到虚拟进度再转回实际进度)
    val miniHeadProgress = (headProgress.asVirtual * 0.995f).asActual
    if (tailProgress >= miniHeadProgress) return null
    val terminalHeadLeft = Tracks.Vertices.onLine(track.bottomTailLeft, miniHeadProgress)
    val terminalHeadRight = Tracks.Vertices.onLine(track.bottomTailRight, miniHeadProgress)
    val miniTailProgress = (tailProgress.asVirtual * 0.995f).asActual
    val terminalTailLeft = Tracks.Vertices.onLine(track.bottomTailLeft, miniTailProgress)
    val terminalTailRight = Tracks.Vertices.onLine(track.bottomTailRight, miniTailProgress)
    val terminalResult = terminalTailLeft to tailLeft

    // 绘制首部
    path(
        color = Colors.Ghost,
        path = Path(arrayOf(terminalHeadLeft, headLeft, headRight, terminalHeadRight)),
        alpha = alpha
    )

    // 绘制链接
    lastResult?.let { (lastTerminalTailLeft, lastTailLeft) ->
        path(
            color = Colors.Ghost,
            path = Path(arrayOf(lastTerminalTailLeft, lastTailLeft, headLeft, terminalHeadLeft)),
            alpha = alpha,
        )
    }

    // 绘制尾部
    path(
        color = Colors.Ghost,
        path = Path(arrayOf(terminalTailLeft, tailLeft, tailRight, terminalTailRight)),
        alpha = alpha
    )

    // 绘制侧边
    val sideWidth = 10f
    path(
        color = Colors.Ghost.copy(alpha = 0.8f),
        path = Path(arrayOf(terminalHeadLeft, tailLeft, tailLeft.translate(x = sideWidth * tailProgress), terminalHeadLeft.translate(x = sideWidth * miniHeadProgress))),
        alpha = alpha
    )
    path(
        color = Colors.Ghost.copy(alpha = 0.8f),
        path = Path(arrayOf(terminalHeadRight, tailRight, tailRight.translate(x = -sideWidth * tailProgress), terminalHeadRight.translate(x = -sideWidth * miniHeadProgress))),
        alpha = alpha
    )

    return terminalResult
}