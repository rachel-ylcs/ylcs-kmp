package love.yinlin.compose.graphics

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush

@Stable
data class Shader(
    val speed: Float,
    val sksl: String
)

@Stable
interface ShaderEffect {
    fun setFloatUniform(name: String, value: Float)
    fun setFloatUniform(name: String, value1: Float, value2: Float)
    fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float)
    fun setFloatUniform(name: String, values: FloatArray)
    fun update(shader: Shader, time: Float, width: Float, height: Float)
    fun build(): Brush
}

expect fun makeShaderEffect(shader: Shader): ShaderEffect?

@Composable
fun ShaderBox(
    shader: Shader,
    modifier: Modifier,
    fallback: @Composable () -> Unit
) {
    val shaderEffect = remember(shader) { makeShaderEffect(shader) }
    if (shaderEffect != null) {
        val speed = shader.speed
        var startMillis = remember(shader) { -1L }
        val time by produceState(0f, speed) {
            while (true) {
                withInfiniteAnimationFrameMillis {
                    if (startMillis < 0) startMillis = it
                    value = ((it - startMillis) / 16.6f) / 10f
                }
            }
        }

        Box(modifier = modifier.drawBehind {
            shaderEffect.update(shader, time * speed, size.width, size.height)
            if (!size.isEmpty()) drawRect(brush = shaderEffect.build())
        })
    }
    else fallback()
}