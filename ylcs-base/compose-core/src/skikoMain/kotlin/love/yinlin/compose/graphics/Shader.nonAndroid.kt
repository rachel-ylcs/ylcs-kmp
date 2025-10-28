package love.yinlin.compose.graphics

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ShaderBrush
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

actual fun makeShaderEffect(shader: Shader): ShaderEffect? = object : ShaderEffect {
    private val mShader = RuntimeShaderBuilder(RuntimeEffect.makeForShader(shader.sksl))
    override fun setFloatUniform(name: String, value: Float) = mShader.uniform(name, value)
    override fun setFloatUniform(name: String, value1: Float, value2: Float) = mShader.uniform(name, value1, value2)
    override fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float) = mShader.uniform(name, value1, value2, value3)
    override fun setFloatUniform(name: String, values: FloatArray) = mShader.uniform(name, values)
    override fun update(shader: Shader, time: Float, width: Float, height: Float) {
        setFloatUniform("uResolution", width, height, width / height)
        setFloatUniform("uTime", time)
    }
    override fun build(): Brush = ShaderBrush(mShader.makeShader())
}