package love.yinlin.compose.graphics

import androidx.compose.ui.graphics.asComposeShader
import org.intellij.lang.annotations.Language
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.jetbrains.skia.impl.use

internal actual fun createShaderBackend(@Language("AGSL") source: String): ShaderBackend {
    val effect = RuntimeEffect.makeForShader(source)
    return { uniforms ->
        { size ->
            RuntimeShaderBuilder(effect).use { builder ->
                for (uniform in uniforms) {
                    val values = uniform.values(size)
                    if (uniform.array) builder.uniform(uniform.name, values)
                    else when (values.size) {
                        1 -> builder.uniform(uniform.name, values[0])
                        2 -> builder.uniform(uniform.name, values[0], values[1])
                        3 -> builder.uniform(uniform.name, values[0], values[1], values[2])
                        4 -> builder.uniform(uniform.name, values[0], values[1], values[2], values[3])
                        else -> error("Invalid float uniform size.")
                    }
                }

                builder.makeShader().asComposeShader()
            }
        }
    }
}
