package love.yinlin.compose.graphics

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import org.intellij.lang.annotations.Language

@Stable
class ShaderProgram(@Language("AGSL") source: String) {
    init {
        require(source.isNotBlank()) { "Shader source must not be empty." }
    }

    private val backend: ShaderBackend = createShaderBackend(source)

    fun brush(uniforms: ShaderUniforms.() -> Unit = {}): ShaderBrush {
        val snapshot = ShaderUniforms().apply(uniforms).snapshot()
        val shaderForSize = backend(snapshot)
        return object : ShaderBrush() {
            override fun createShader(size: Size): Shader = shaderForSize(size)
        }
    }
}
