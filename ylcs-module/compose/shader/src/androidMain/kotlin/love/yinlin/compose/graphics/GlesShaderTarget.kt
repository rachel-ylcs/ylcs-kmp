package love.yinlin.compose.graphics

import android.graphics.Bitmap
import android.opengl.GLES20
import java.nio.ByteBuffer

internal class GlesShaderTarget(val width: Int, val height: Int) {
    val pixels: ByteBuffer = ByteBuffer.allocateDirect(Math.multiplyExact(Math.multiplyExact(width, height), 4))
    private val objects = IntArray(2)
    val framebuffer: Int get() = objects[0]
    private val texture: Int get() = objects[1]

    init {
        GLES20.glGenFramebuffers(1, objects, 0)
        GLES20.glGenTextures(1, objects, 1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null,
        )
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer)
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texture, 0,
        )
        val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if (framebuffer == 0 || texture == 0 || status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            close()
            error("Could not allocate a ${width}x${height} shader framebuffer (0x${status.toString(16)}).")
        }
    }

    fun copyTo(bitmap: Bitmap) {
        val packedRowBytes = width * 4
        if (bitmap.rowBytes == packedRowBytes) bitmap.copyPixelsFromBuffer(pixels)
        else {
            val paddedPixels = ByteBuffer.allocateDirect(bitmap.byteCount)
            val row = pixels.duplicate()
            for (y in 0 ..< height) {
                val start = y * packedRowBytes
                row.limit(start + packedRowBytes)
                row.position(start)
                paddedPixels.position(y * bitmap.rowBytes)
                paddedPixels.put(row)
            }
            paddedPixels.rewind()
            bitmap.copyPixelsFromBuffer(paddedPixels)
        }
    }

    fun close() {
        GLES20.glDeleteFramebuffers(1, objects, 0)
        GLES20.glDeleteTextures(1, objects, 1)
    }
}
