package love.yinlin.compose.graphics

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shader
import org.intellij.lang.annotations.Language

internal typealias ShaderBackend = (List<ShaderUniform>) -> (Size) -> Shader

internal expect fun createShaderBackend(@Language("AGSL") source: String): ShaderBackend
