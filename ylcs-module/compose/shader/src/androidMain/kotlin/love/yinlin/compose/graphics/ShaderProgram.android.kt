package love.yinlin.compose.graphics

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shader

internal actual fun createShaderBackend(source: String): (List<ShaderUniform>) -> (Size) -> Shader =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) androidRuntimeShader(source)
    else glesRuntimeShader(source)

private fun glesRuntimeShader(source: String): (List<ShaderUniform>) -> (Size) -> Shader {
    val fragmentSource = toGlesFragmentSource(source)
    return { uniforms ->
        { size -> GlesShaderRenderer.render(fragmentSource, uniforms, size) }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun androidRuntimeShader(source: String): (List<ShaderUniform>) -> (Size) -> Shader = { uniforms ->
    if (uniforms.any { it.dependsOnSize }) {
        { size -> createAndroidShader(source, uniforms, size) }
    }
    else {
        val shader by lazy(LazyThreadSafetyMode.NONE) { createAndroidShader(source, uniforms, Size.Zero) }
        val shaderForSize: (Size) -> Shader = { shader }
        shaderForSize
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun createAndroidShader(source: String, uniforms: List<ShaderUniform>, size: Size): Shader {
    val shader = RuntimeShader(source)
    for (uniform in uniforms) {
        val values = uniform.values(size)
        if (uniform.array) shader.setFloatUniform(uniform.name, values)
        else when (values.size) {
            1 -> shader.setFloatUniform(uniform.name, values[0])
            2 -> shader.setFloatUniform(uniform.name, values[0], values[1])
            3 -> shader.setFloatUniform(uniform.name, values[0], values[1], values[2])
            4 -> shader.setFloatUniform(uniform.name, values[0], values[1], values[2], values[3])
            else -> error("Invalid float uniform size.")
        }
    }
    return shader
}
