@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.jetbrains.skiko

import kotlinx.cinterop.*
import org.jetbrains.skia.ExternalSymbolName
import org.jetbrains.skia.Surface
import org.jetbrains.skia.impl.Native.Companion.NullPointer
import org.jetbrains.skia.impl.NativePointer

/**
 * Owns a Ganesh Vulkan device and swapchain for a toolkit's Linux window.
 *
 * [surfaceFactory] is a native `void* (*)(void* userData, void* instance)` callback returning
 * a `VkSurfaceKHR`, or null on failure. It must use the supplied instance and leave destruction
 * to Skiko. [userData] and the toolkit window must remain valid until [close]. All calls run on
 * the window thread. The host's [LinuxSkiaLayerComponent.createVulkanWindow] transfers ownership
 * to the renderer, which closes this object when the layer detaches or its renderer changes.
 */
@InternalSkikoApi
class LinuxVulkanWindow(
    instanceExtensions: List<String>,
    surfaceFactory: NativePointer,
    userData: NativePointer,
    properties: SkiaLayerProperties = SkiaLayerProperties(),
) {
    private var pointer = memScoped {
        require(instanceExtensions.size in 1 .. 64)
        require(instanceExtensions.all { it.isNotEmpty() && '\u0000' !in it })
        require(surfaceFactory != NullPointer)
        val names = allocArray<CPointerVar<ByteVar>>(instanceExtensions.size)
        instanceExtensions.forEachIndexed { index, name -> names[index] = name.cstr.getPointer(this) }
        create(names.rawValue, instanceExtensions.size, surfaceFactory, userData,
            properties.adapterPriority.ordinal, properties.frameBuffering.numberOfBuffers() ?: 0,
            properties.gpuResourceCacheLimit).also {
            if (it == NullPointer) throw RenderException(vulkanError())
        }
    }

    val deviceName: String
        get() {
            checkOpen()
            return interpretCPointer<ByteVar>(deviceName(pointer))?.toKString() ?: "Unknown Vulkan device"
        }

    internal fun acquire(width: Int, height: Int, vsync: Boolean): Surface {
        checkOpen()
        val surface = acquire(pointer, width, height, vsync)
        if (surface == NullPointer) throw RenderException(vulkanError())
        return Surface(surface)
    }

    internal fun present() {
        checkOpen()
        if (!present(pointer)) throw RenderException(vulkanError())
    }

    internal fun isContextLost(): Boolean = pointer == NullPointer || isLost(pointer)

    fun close() {
        if (pointer == NullPointer) return
        destroy(pointer)
        pointer = NullPointer
    }

    private fun checkOpen() = check(pointer != NullPointer) { "Vulkan window is closed" }
}

private fun vulkanError(): String = interpretCPointer<ByteVar>(lastError())?.toKString() ?: "Vulkan window operation failed"

@ExternalSymbolName("RachelVulkanWindow_Create")
private external fun create(names: NativePointer, count: Int, factory: NativePointer, userData: NativePointer,
    priority: Int, buffers: Int, cacheLimit: Long): NativePointer
@ExternalSymbolName("RachelVulkanWindow_Destroy")
private external fun destroy(pointer: NativePointer)
@ExternalSymbolName("RachelVulkanWindow_Acquire")
private external fun acquire(pointer: NativePointer, width: Int, height: Int, vsync: Boolean): NativePointer
@ExternalSymbolName("RachelVulkanWindow_Present")
private external fun present(pointer: NativePointer): Boolean
@ExternalSymbolName("RachelVulkanWindow_IsLost")
private external fun isLost(pointer: NativePointer): Boolean
@ExternalSymbolName("RachelVulkanWindow_DeviceName")
private external fun deviceName(pointer: NativePointer): NativePointer
@ExternalSymbolName("RachelVulkanWindow_LastError")
private external fun lastError(): NativePointer
