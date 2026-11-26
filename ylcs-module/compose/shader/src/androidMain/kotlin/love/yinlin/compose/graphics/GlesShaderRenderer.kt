package love.yinlin.compose.graphics

import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Shader
import android.opengl.GLES20
import androidx.compose.ui.geometry.Size
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil
import androidx.core.graphics.createBitmap

internal object GlesShaderRenderer {
    private const val MAX_PROGRAMS = 16
    private const val VERTEX_SOURCE = """
        attribute vec2 position;
        void main() {
            gl_Position = vec4(position, 0.0, 1.0);
        }
    """

    private val context by lazy(LazyThreadSafetyMode.NONE) { GlesShaderContext() }
    private val programs = LinkedHashMap<String, GlesShaderProgram>(MAX_PROGRAMS, 0.75f, true)
    private val vertices = ByteBuffer.allocateDirect(6 * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            val positions: FloatArray = [-1f, -1f, 3f, -1f, -1f, 3f]
            put(positions)
            rewind()
        }

    private var target: GlesShaderTarget? = null
    private var maximumWidth = 0
    private var maximumHeight = 0

    @Synchronized
    fun render(source: String, uniforms: List<ShaderUniform>, size: Size): Shader = context.withCurrent {
        require(size.width.isFinite() && size.height.isFinite() && !size.isEmpty()) { "A shader brush requires a finite, positive draw size." }

        if (maximumWidth == 0) {
            val limits = IntArray(2)
            GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, limits, 0)

            val maxTexture = limits[0]
            GLES20.glGetIntegerv(GLES20.GL_MAX_VIEWPORT_DIMS, limits, 0)

            maximumWidth = minOf(maxTexture, limits[0])
            maximumHeight = minOf(maxTexture, limits[1])

            check(maximumWidth > 0 && maximumHeight > 0) { "Could not determine the offscreen drawing limits." }

            GLES20.glDisable(GLES20.GL_DITHER)
        }

        val scale = minOf(1f, maximumWidth / size.width, maximumHeight / size.height)
        val width = ceil(size.width * scale).toInt().coerceIn(1, maximumWidth)
        val height = ceil(size.height * scale).toInt().coerceIn(1, maximumHeight)
        val target = targetFor(width, height)
        val program = programFor(source)
        val scaleX = size.width / width
        val scaleY = size.height / height

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, target.framebuffer)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glUseProgram(program.id)
        GLES20.glUniform2f(program.pixelScale, scaleX, scaleY)

        program.bindUniforms(uniforms, size)

        GLES20.glVertexAttribPointer(0, 2, GLES20.GL_FLOAT, false, 0, vertices)
        GLES20.glEnableVertexAttribArray(0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
        GLES20.glDisableVertexAttribArray(0)

        target.pixels.clear()
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, target.pixels)

        val error = GLES20.glGetError()
        check(error == GLES20.GL_NO_ERROR) { "OpenGL ES error 0x${error.toString(16)} while rendering to the shader bitmap." }

        val bitmap = createBitmap(width, height)
        target.pixels.rewind()
        target.copyTo(bitmap)

        BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            if (scaleX != 1f || scaleY != 1f) setLocalMatrix(Matrix().apply { setScale(scaleX, scaleY) })
        }
    }

    private fun targetFor(width: Int, height: Int): GlesShaderTarget {
        val current = target
        if (current != null && current.width == width && current.height == height) return current

        val replacement = GlesShaderTarget(width, height)
        current?.close()
        target = replacement
        return replacement
    }

    private fun programFor(source: String): GlesShaderProgram {
        programs[source]?.let { return it }

        val program = GlesShaderProgram(VERTEX_SOURCE, source)
        if (programs.size == MAX_PROGRAMS) {
            val oldest = programs.entries.iterator()
            oldest.next().value.close()
            oldest.remove()
        }
        programs[source] = program

        return program
    }
}
