package love.yinlin.compose.game.layer

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.common.Moment
import love.yinlin.compose.game.common.Moments
import love.yinlin.compose.game.data.RhymePlayInfo
import love.yinlin.compose.game.drawer.LayerType
import love.yinlin.compose.game.traits.Layer
import love.yinlin.compose.game.visible.InteractTipArea
import love.yinlin.media.AudioPlayer

// 时刻层
@Stable
class MomentLayer(
    playInfo: RhymePlayInfo,
    private val player: AudioPlayer,
    private val uiLayer: UILayer
) : Layer(layerOrder = 1, layerType = LayerType.Absolute) {
    override val interactive: Boolean = false

    // 音轨位置
    var audioPosition: Long = 0L
        private set
    // 音轨时长
    private var audioDuration: Long = 0L

    // 时刻表
    private val moments = Moments(
        Moment(3000L) { InteractTipArea() }
    )

    override fun preUpdate(tick: Int) {
        // 更新进度
        val currentAudioPosition = player.position
        val currentAudioDuration = player.duration
        audioPosition = currentAudioPosition
        audioDuration = currentAudioDuration

        // 更新封面进度
        uiLayer.uiCover.updateAudioPosition(currentAudioPosition, currentAudioDuration)

        // 时刻
        moments.check(currentAudioPosition) { this += it }
    }
}