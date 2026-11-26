package love.yinlin.compose.graphics

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush

/**
 * Reusable source for a Compose brush. Use the common subset of AGSL and SkSL to share source.
 * On Android 9–12 the subset is further limited by the GLES 2 fragment shader language.
 */
@Stable
class ShaderProgram(source: String) {
    init {
        require(source.isNotBlank()) { "Shader source must not be empty." }
    }

    private val backend = createShaderBackend(source)

    /**
     * Captures the given uniforms for this brush. Create another brush when their values change.
     * Size uniforms use the pixel dimensions of the current Compose draw operation.
     */
    fun brush(uniforms: ShaderUniforms.() -> Unit = {}): ShaderBrush {
        val snapshot = ShaderUniforms().apply(uniforms).snapshot()
        val shaderForSize = backend(snapshot)
        return object : ShaderBrush() {
            override fun createShader(size: Size): Shader = shaderForSize(size)
        }
    }
}

internal expect fun createShaderBackend(source: String): (List<ShaderUniform>) -> (Size) -> Shader
