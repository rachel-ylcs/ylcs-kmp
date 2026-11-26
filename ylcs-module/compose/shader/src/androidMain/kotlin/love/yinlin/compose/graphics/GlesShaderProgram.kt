package love.yinlin.compose.graphics

import android.opengl.GLES20
import androidx.compose.ui.geometry.Size

internal class GlesShaderProgram(vertexSource: String, fragmentSource: String) {
    val id = linkProgram(vertexSource, fragmentSource)
    val pixelScale = GLES20.glGetUniformLocation(id, "rachelShaderPixelScale")
    private val uniforms = readUniforms()

    fun bindUniforms(values: List<ShaderUniform>, size: Size) {
        for (uniform in uniforms) {
            val input = values.firstOrNull { it.name == uniform.name }
            val data = input?.values?.invoke(size) ?: uniform.defaults
            require(input == null || (input.array == uniform.array && if (uniform.array) data.size >= uniform.count else data.size == uniform.defaults.size)) {
                "Uniform '${uniform.name}' does not match its declaration."
            }
            val location = uniform.location
            when (uniform.type) {
                GLES20.GL_FLOAT -> GLES20.glUniform1fv(location, uniform.count, data, 0)
                GLES20.GL_FLOAT_VEC2 -> GLES20.glUniform2fv(location, uniform.count, data, 0)
                GLES20.GL_FLOAT_VEC3 -> GLES20.glUniform3fv(location, uniform.count, data, 0)
                GLES20.GL_FLOAT_VEC4 -> GLES20.glUniform4fv(location, uniform.count, data, 0)
            }
        }
    }

    private fun readUniforms(): List<GlesShaderUniform> {
        val count = IntArray(1)
        GLES20.glGetProgramiv(id, GLES20.GL_ACTIVE_UNIFORMS, count, 0)
        val length = IntArray(1)
        val type = IntArray(1)
        try {
            return buildList {
                for (index in 0 ..< count[0]) {
                    val glName = GLES20.glGetActiveUniform(id, index, length, 0, type, 0)
                    if (glName == "rachelShaderPixelScale") continue
                    val name = glName.removeSuffix("[0]")
                    val array = name != glName || length[0] > 1
                    val components = when (type[0]) {
                        GLES20.GL_FLOAT -> 1
                        GLES20.GL_FLOAT_VEC2 -> 2
                        GLES20.GL_FLOAT_VEC3 -> 3
                        GLES20.GL_FLOAT_VEC4 -> 4
                        else -> error("Uniform '$name' has an unsupported type on Android 9–12.")
                    }
                    require(!array || components == 1) { "Uniform '$name' must be a scalar float array on Android 9–12." }
                    add(GlesShaderUniform(name, GLES20.glGetUniformLocation(id, glName), type[0], length[0], array, FloatArray(components * length[0])))
                }
            }
        } catch (failure: RuntimeException) {
            close()
            throw failure
        }
    }

    fun close() = GLES20.glDeleteProgram(id)
}

private class GlesShaderUniform(
    val name: String,
    val location: Int,
    val type: Int,
    val count: Int,
    val array: Boolean,
    val defaults: FloatArray,
)

private fun linkProgram(vertexSource: String, fragmentSource: String): Int {
    val vertex = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
    try {
        val fragment = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        try {
            val program = GLES20.glCreateProgram()
            check(program != 0) { "Could not allocate a shader program." }
            GLES20.glAttachShader(program, vertex)
            GLES20.glAttachShader(program, fragment)
            GLES20.glBindAttribLocation(program, 0, "position")
            GLES20.glLinkProgram(program)
            val status = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
            if (status[0] == GLES20.GL_TRUE) return program
            val log = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            error("Could not link the shader on Android 9–12: $log")
        } finally {
            GLES20.glDeleteShader(fragment)
        }
    } finally {
        GLES20.glDeleteShader(vertex)
    }
}

private fun compileShader(type: Int, source: String): Int {
    val shader = GLES20.glCreateShader(type)
    check(shader != 0) { "Could not allocate a shader." }
    GLES20.glShaderSource(shader, source)
    GLES20.glCompileShader(shader)
    val status = IntArray(1)
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
    if (status[0] == GLES20.GL_TRUE) return shader
    val log = GLES20.glGetShaderInfoLog(shader)
    GLES20.glDeleteShader(shader)
    error("Could not compile the shader on Android 9–12: $log")
}
