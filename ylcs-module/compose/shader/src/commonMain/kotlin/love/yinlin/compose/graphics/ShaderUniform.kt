package love.yinlin.compose.graphics

import androidx.compose.ui.geometry.Size

internal class ShaderUniform(
    val name: String,
    val array: Boolean = false,
    val dependsOnSize: Boolean = false,
    val values: (Size) -> FloatArray,
)
