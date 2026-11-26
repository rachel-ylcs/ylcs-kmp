package love.yinlin.compose.graphics

class ShaderUniforms internal constructor() {
    private val values = mutableMapOf<String, ShaderUniform>()

    fun float(name: String, value: Float) = put(name, [value])
    fun float2(name: String, x: Float, y: Float) = put(name, [x, y])
    fun float3(name: String, x: Float, y: Float, z: Float) = put(name, [x, y, z])
    fun float4(name: String, x: Float, y: Float, z: Float, w: Float) = put(name, [x, y, z, w])

    fun floatArray(name: String, values: FloatArray) {
        require(values.isNotEmpty()) { "Uniform array '$name' must contain at least one value." }
        put(name, values.copyOf(), array = true)
    }

    fun resolution(name: String) {
        values[name] = ShaderUniform(name, dependsOnSize = true) { [it.width, it.height] }
    }

    fun resolutionWithAspectRatio(name: String) {
        values[name] = ShaderUniform(name, dependsOnSize = true) { [it.width, it.height, it.width / it.height] }
    }

    internal fun snapshot(): List<ShaderUniform> = values.values.toList()

    private fun put(name: String, values: FloatArray, array: Boolean = false) {
        this.values[name] = ShaderUniform(name, array) { values }
    }
}
