package love.yinlin.compose.graphics

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ShaderBrush

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

actual fun makeShaderEffect(shader: Shader): ShaderEffect? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) SkiaNativeShaderEffect(shader) else null