package love.yinlin.compose.graphics

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ShaderBrush
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class SkiaNativeShaderEffect(shader: Shader) : ShaderEffect {
    private val mShader = RuntimeShader(shader.sksl)
    override fun setFloatUniform(name: String, value: Float) = mShader.setFloatUniform(name, value)
    override fun setFloatUniform(name: String, value1: Float, value2: Float) = mShader.setFloatUniform(name, value1, value2)
    override fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float) = mShader.setFloatUniform(name, value1, value2, value3)
    override fun setFloatUniform(name: String, values: FloatArray) = mShader.setFloatUniform(name, values)
    override fun update(shader: Shader, time: Float, width: Float, height: Float) {
        setFloatUniform("uResolution", width, height, width / height)
        setFloatUniform("uTime", time)
    }
    override fun build(): Brush = ShaderBrush(mShader)
}

private class SkiaLibShaderEffect(shader: Shader) : ShaderEffect {
    private val mShader = RuntimeShaderBuilder(RuntimeEffect.makeForShader(shader.sksl))
    override fun setFloatUniform(name: String, value: Float) = mShader.uniform(name, value)
    override fun setFloatUniform(name: String, value1: Float, value2: Float) = mShader.uniform(name, value1, value2)
    override fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float) = mShader.uniform(name, value1, value2, value3)
    override fun setFloatUniform(name: String, values: FloatArray) = mShader.uniform(name, values)
    override fun update(shader: Shader, time: Float, width: Float, height: Float) {
        setFloatUniform("uResolution", width, height, width / height)
        setFloatUniform("uTime", time)
    }
    override fun build(): Brush = object : ShaderBrush() {
        override fun createShader(size: Size) = TODO("")
    }
}

actual fun makeShaderEffect(shader: Shader): ShaderEffect? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) SkiaNativeShaderEffect(shader) else null