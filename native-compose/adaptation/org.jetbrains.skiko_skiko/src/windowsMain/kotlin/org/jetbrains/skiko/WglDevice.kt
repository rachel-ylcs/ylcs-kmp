@file:OptIn(ExperimentalForeignApi::class)

package org.jetbrains.skiko

import kotlinx.cinterop.*
import platform.windows.*

internal data class WglDevice(
    val deviceContext: HDC,
    val stencilBits: Int,
    val doubleBuffered: Boolean,
    val hasTransparentWindowBuffer: Boolean,
)

internal fun createDeviceContext(
    window: WindowsNativeWindow,
    transparencyRequested: Boolean,
): WglDevice {
    val deviceContext = GetDC(window.hwnd()) ?: throw RenderException("GetDC failed (Win32 error ${GetLastError()})")
    try {
        return memScoped {
            var pixelFormat = GetPixelFormat(deviceContext)
            val descriptor = alloc<PIXELFORMATDESCRIPTOR>()
            descriptor.nSize = sizeOf<PIXELFORMATDESCRIPTOR>().convert()
            descriptor.nVersion = 1.convert()

            if (pixelFormat == 0) {
                descriptor.dwFlags = (PFD_DRAW_TO_WINDOW or PFD_SUPPORT_OPENGL or PFD_DOUBLEBUFFER or if (transparencyRequested) PFD_SUPPORT_COMPOSITION else 0).convert()
                descriptor.iPixelType = PFD_TYPE_RGBA.convert()
                descriptor.cColorBits = 32.convert()
                descriptor.cAlphaBits = 8.convert()
                descriptor.cDepthBits = 0.convert()
                descriptor.cStencilBits = 8.convert()

                pixelFormat = ChoosePixelFormat(deviceContext, descriptor.ptr)
                if (pixelFormat == 0 || SetPixelFormat(deviceContext, pixelFormat, descriptor.ptr) == 0) {
                    throw RenderException("Could not configure a WGL pixel format (Win32 error ${GetLastError()})")
                }
            }

            if (DescribePixelFormat(deviceContext, pixelFormat, sizeOf<PIXELFORMATDESCRIPTOR>().convert(), descriptor.ptr) == 0) {
                throw RenderException("Could not inspect WGL pixel format $pixelFormat (Win32 error ${GetLastError()})")
            }
            val requiredFlags = PFD_DRAW_TO_WINDOW or PFD_SUPPORT_OPENGL
            if (descriptor.dwFlags.toInt() and requiredFlags != requiredFlags) {
                throw RenderException("HWND pixel format $pixelFormat cannot host windowed OpenGL")
            }
            val hasTransparentWindowBuffer = descriptor.cAlphaBits.toInt() >= 8 && descriptor.dwFlags.toInt() and PFD_SUPPORT_COMPOSITION != 0
            if (transparencyRequested && !hasTransparentWindowBuffer) {
                throw RenderException("HWND pixel format $pixelFormat does not expose an alpha-composited WGL buffer")
            }
            WglDevice(
                deviceContext = deviceContext,
                stencilBits = when {
                    descriptor.cStencilBits.toInt() >= 16 -> 16
                    descriptor.cStencilBits.toInt() >= 8 -> 8
                    else -> 0
                },
                doubleBuffered = descriptor.dwFlags.toInt() and PFD_DOUBLEBUFFER != 0,
                hasTransparentWindowBuffer = hasTransparentWindowBuffer,
            )
        }
    }
    catch (failure: Throwable) {
        ReleaseDC(window.hwnd(), deviceContext)
        throw failure
    }
}