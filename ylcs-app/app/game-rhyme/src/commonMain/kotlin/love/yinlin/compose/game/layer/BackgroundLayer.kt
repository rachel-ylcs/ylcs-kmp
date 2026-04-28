package love.yinlin.compose.game.layer

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import love.yinlin.compose.game.character.Character
import love.yinlin.compose.game.drawer.LayerType
import love.yinlin.compose.game.traits.Layer
import love.yinlin.compose.game.visible.BackgroundConstellation
import love.yinlin.compose.game.visible.BackgroundRipple
import love.yinlin.compose.game.visible.BackgroundWave

@Stable
class BackgroundLayer(private val character: Character) : Layer(
    BackgroundConstellation(layerOrder = 1),
    BackgroundWave(
        waveColor = Color(0xFF00E5FF).copy(alpha = 0.1f),
        phaseRatio = 0.003f,
        phi = 0f,
        frequency = 0.001f,
        amplitudeRatio = 1f,
        layerOrder = 2
    ),
    BackgroundRipple(layerOrder = 3),
    BackgroundWave(
        waveColor = Color(0xFFFF00FF).copy(alpha = 0.1f),
        phaseRatio = 0.002f,
        phi = 3.141592f,
        frequency = 0.0012f,
        amplitudeRatio = 0.8f,
        layerOrder = 4
    ),
    layerOrder = 0,
    layerType = LayerType.Absolute
) {
    override val interactive: Boolean = false
}