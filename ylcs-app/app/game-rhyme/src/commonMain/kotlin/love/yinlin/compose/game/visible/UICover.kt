package love.yinlin.compose.game.visible

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import love.yinlin.compose.Colors
import love.yinlin.compose.game.data.RhymePlayInfo
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.compose.game.event.Event
import love.yinlin.compose.game.event.PointerEventListener
import love.yinlin.compose.game.layer.UILayer
import love.yinlin.compose.game.traits.AABB
import love.yinlin.compose.game.traits.Trigger
import love.yinlin.compose.game.traits.Visible

class UICover(private val info: RhymePlayInfo) : Visible(position = DefaultPosition, size = DefaultSize, aabb = AABB.Circle) {
    companion object {
        private const val RADIUS = (UILayer.DEFAULT_HEIGHT - UILayer.DEFAULT_PADDING) / 2
        private val DefaultPosition = Offset(UILayer.DEFAULT_PADDING, UILayer.DEFAULT_PADDING)
        val DefaultSize = Size(RADIUS * 2, RADIUS * 2)
        private val DefaultRect = Rect(Offset.Zero, DefaultSize)
        private val DefaultStroke = Stroke(20f, cap = StrokeCap.Round)
        private val NormalColor = Colors.White.copy(alpha = 0.85f)
        private val ActiveColor = Colors.Green4
        private val ClipPath = Path().apply { addOval(DefaultRect) }
    }

    private var progress = 0f
    private var lastAudioPosition = 0L

    override val trigger: Trigger = Trigger(
        object : PointerEventListener() {
            override fun onPointerUp(event: Event.Pointer.Up) {
                layer?.scene?.engine?.isRunning = false
            }
        }
    )

    fun updateAudioPosition(audioPosition: Long, audioDuration: Long) {
        // 降频
        if (audioPosition - lastAudioPosition > 1000L) {
            progress = if (audioDuration == 0L) 0f else audioPosition / audioDuration.toFloat()
            lastAudioPosition = audioPosition
            updateDirty()
        }
    }

    override fun Drawer.onDraw() {
        // 时长
        arc(NormalColor, -90f, 360f, Offset.Zero, DefaultSize, style = DefaultStroke)
        // 进度
        arc(ActiveColor, -90f, 360f * progress, Offset.Zero, DefaultSize, style = DefaultStroke)
        // 封面
        clip(ClipPath) { image(info.musicRecord, DefaultRect) }
    }
}