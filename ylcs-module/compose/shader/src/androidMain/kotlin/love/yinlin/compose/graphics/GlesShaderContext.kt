package love.yinlin.compose.graphics

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLSurface

internal class GlesShaderContext {
    private val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    private val context: EGLContext
    private val surface: EGLSurface

    init {
        check(display != EGL14.EGL_NO_DISPLAY && EGL14.eglInitialize(display, null, 0, null, 0)) { eglError("initialize") }
        val configs = arrayOfNulls<EGLConfig>(1)
        val count = IntArray(1)
        val configAttributes: IntArray = [
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE,
        ]
        check(EGL14.eglChooseConfig(display, configAttributes, 0, configs, 0, configs.size, count, 0) && count[0] > 0) {
            eglError("choose an offscreen configuration")
        }
        val config = checkNotNull(configs[0])
        context = EGL14.eglCreateContext(
            display,
            config,
            EGL14.EGL_NO_CONTEXT,
            [EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE],
            0,
        )
        check(context != EGL14.EGL_NO_CONTEXT) { eglError("create a context") }
        surface = EGL14.eglCreatePbufferSurface(
            display,
            config,
            [EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE],
            0,
        )
        if (surface == EGL14.EGL_NO_SURFACE) {
            val message = eglError("create an offscreen surface")
            EGL14.eglDestroyContext(display, context)
            error(message)
        }
    }

    fun <T> withCurrent(block: () -> T): T {
        val previousDisplay = EGL14.eglGetCurrentDisplay()
        val previousContext = EGL14.eglGetCurrentContext()
        val previousDraw = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW)
        val previousRead = EGL14.eglGetCurrentSurface(EGL14.EGL_READ)
        check(EGL14.eglMakeCurrent(display, surface, surface, context)) { eglError("bind the shader context") }
        try {
            return block()
        } finally {
            val restored = if (previousDisplay == EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            }
            else EGL14.eglMakeCurrent(previousDisplay, previousDraw, previousRead, previousContext)
            check(restored) { eglError("restore the previous context") }
        }
    }

    private fun eglError(operation: String): String =
        "Could not $operation (EGL error 0x${EGL14.eglGetError().toString(16)})."
}
