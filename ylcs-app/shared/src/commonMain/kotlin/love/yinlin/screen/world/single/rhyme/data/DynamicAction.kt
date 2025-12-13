package love.yinlin.screen.world.single.rhyme.data

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import love.yinlin.compose.Colors
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.animation.FrameAnimation
import love.yinlin.compose.graphics.AnimatedWebp
import love.yinlin.compose.graphics.SolidColorFilter
import love.yinlin.data.music.RhymeAction

@Stable
sealed interface DynamicAction {
    companion object {
        const val BASE_DURATION = 3000L // 音符持续时间
        const val BASE_DURATION_F = BASE_DURATION.toFloat()

        const val PERSPECTIVE_K = 3 // 透视参数
        const val HIT_RATIO = 0.8f // 判定线

        val deadline = ActionResult.BAD.endRange(HIT_RATIO) // 死线

        val BlockSize = Size(256f, 85f)
        fun calcBlockRect(scale: Int, base: Int): Pair<Rect, Rect> {
            val left = Rect(Offset(0f, (scale + base) * BlockSize.height), BlockSize)
            val center = Rect(Offset(BlockSize.width, (scale + base) * BlockSize.height), BlockSize)
            return left to center
        }

        fun mapTrackIndex(scale: Byte): Int = Tracks.Scales.indexOf((scale - 1) % 7 + 1)

        fun mapNoteScale(scale: Byte): Int = (scale - 1) / 7

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

    var bindId: Long? // 绑定指针ID
    val action: RhymeAction // 行为
    val appearance: Long // 出现时刻
    val isDismiss: Boolean // 消失

    fun onAdmission() // 入场
    fun onUpdate(tick: Long, callback: ActionCallback) // 更新
    fun onTrackDown(track: Track, tick: Long, callback: ActionCallback): Boolean // 按下
    fun onTrackUp(track: Track, tick: Long, callback: ActionCallback) // 抬起
    fun onTrackTransfer(oldTrack: Track, newTrack: Track, tick: Long, callback: ActionCallback): Boolean // 变轨
    fun Drawer.onDraw() // 渲染
}

internal val Float.asUncheckedActual: Float get() = this / (DynamicAction.PERSPECTIVE_K + this * (1 - DynamicAction.PERSPECTIVE_K))
internal val Float.asActual: Float get() = this.coerceIn(0f, 1f).asUncheckedActual.coerceIn(0f, 1f)
internal val Float.asUncheckedVirtual: Float get() = DynamicAction.PERSPECTIVE_K * this / (1 + this * (DynamicAction.PERSPECTIVE_K - 1))
internal val Float.asVirtual: Float get() = this.coerceIn(0f, 1f).asUncheckedVirtual.coerceIn(0f, 1f)

// 音符透视
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

// 画平铺动画
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

// 画透视动画
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