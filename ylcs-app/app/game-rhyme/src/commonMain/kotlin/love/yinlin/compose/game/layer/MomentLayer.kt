package love.yinlin.compose.game.layer

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.character.Character
import love.yinlin.compose.game.character.CharacterLiDiShiGongFenB
import love.yinlin.compose.game.common.Moments
import love.yinlin.compose.game.data.RhymeDifficulty
import love.yinlin.compose.game.data.RhymePlayInfo
import love.yinlin.compose.game.drawer.LayerType
import love.yinlin.compose.game.traits.Layer
import love.yinlin.compose.game.visible.InteractTipArea
import love.yinlin.media.AudioPlayer

// 时刻层
@Stable
class MomentLayer(
    character: Character,
    playInfo: RhymePlayInfo,
    private val player: AudioPlayer
) : Layer(layerOrder = 1, layerType = LayerType.Absolute) {
    override val interactive: Boolean = false

    private val difficulty = playInfo.playConfig.difficulty

    // 音轨位置
    var audioPosition: Long = 0L
        private set
    // 音轨时长
    var audioDuration: Long = 0L
        private set

    // 时刻表
    private val moments = Moments {
        val difficultyEnabled = difficulty == RhymeDifficulty.Easy || difficulty == RhymeDifficulty.Medium
        if (difficultyEnabled && character !is CharacterLiDiShiGongFenB) {
            moment(3000L, ::InteractTipArea)
        }
    }

    override fun preUpdate(tick: Int) {
        val currentAudioPosition = player.position
        val currentAudioDuration = player.duration
        audioPosition = currentAudioPosition
        audioDuration = currentAudioDuration

        // 时刻
        moments.check(currentAudioPosition) { this += it }
    }
}