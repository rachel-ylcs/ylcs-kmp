package love.yinlin.compose.game.layer

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import love.yinlin.compose.game.character.Character
import love.yinlin.compose.game.character.CharacterLiDiShiGongFenB
import love.yinlin.compose.game.drawer.LayerType
import love.yinlin.compose.game.traits.Layer
import love.yinlin.compose.game.traits.Visible
import love.yinlin.compose.game.visible.BackgroundConstellation
import love.yinlin.compose.game.visible.BackgroundRipple
import love.yinlin.compose.game.visible.BackgroundWave

@Stable
class BackgroundLayer(
    character: Character,
    characterImage: ImageBitmap?,
) : Layer(
    *buildList<Visible> {
        if (character !is CharacterLiDiShiGongFenB) {
            add(BackgroundConstellation(layerOrder = 1))
            add(BackgroundWave(
                waveColor = Color(0xFF00E5FF),
                phaseRatio = 0.003f,
                phi = 0f,
                frequency = 0.001f,
                amplitudeRatio = 1f,
                layerOrder = 2
            ))
            add(BackgroundRipple(
                character = character,
                characterImage = characterImage,
                skillDuration = 500,
                layerOrder = 3
            ))
            add(BackgroundWave(
                waveColor = Color(0xFFFF00FF),
                phaseRatio = 0.002f,
                phi = 3.141592f,
                frequency = 0.0012f,
                amplitudeRatio = 0.8f,
                layerOrder = 4
            ))
        }
    }.toTypedArray(),
    layerOrder = 0,
    layerType = LayerType.Absolute
) {
    override val interactive: Boolean = false

    fun activateSkill() = findVisible<BackgroundRipple>()?.activateSkill()
    fun updateSkill() = findVisible<BackgroundRipple>()?.updateSkill()
}