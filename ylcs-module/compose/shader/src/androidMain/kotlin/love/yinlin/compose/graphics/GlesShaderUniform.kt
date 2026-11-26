package love.yinlin.compose.graphics

internal class GlesShaderUniform(
    val name: String,
    val location: Int,
    val type: Int,
    val count: Int,
    val array: Boolean,
    val defaults: FloatArray,
)