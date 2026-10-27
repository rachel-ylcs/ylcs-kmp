package org.jetbrains.skiko.resources

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import org.jetbrains.skiko.internal.sdl.*

/** SDL resource services without exposing target-specific CInterop types to dependent libraries. */
@org.jetbrains.skiko.InternalSkikoApi
object DesktopResourceSupport {
    fun baseDirectory(): String = checkNotNull(SDL_GetBasePath()?.toKString()) { "Cannot locate the Native application's resource directory" }

    fun displayScale(): Float {
        val display = if (SDL_IsMainThread() && SDL_WasInit(SDL_INIT_VIDEO) != 0u) SDL_GetPrimaryDisplay() else 0u
        return if (display != 0u) SDL_GetDisplayContentScale(display).takeIf { it.isFinite() && it > 0f } ?: 1f else 1f
    }

    fun isFile(path: String): Boolean {
        require('\u0000' !in path) { "File path contains a NUL character" }
        return RachelSDL_IsFile(path)
    }

    fun readFile(path: String, offset: Long, requestedSize: Long?): ByteArray? {
        require('\u0000' !in path) { "File path contains a NUL character" }
        require(offset >= 0 && (requestedSize == null || requestedSize >= 0)) { "Resource offset and size must be non-negative" }
        val stream = RachelSDL_OpenFile(path, "rb") ?: return null
        try {
            val length = RachelSDL_GetFileSize(stream)
            if (length < 0 || offset > length) return null
            val available = length - offset
            val size = requestedSize ?: available
            if (size > available || size > Int.MAX_VALUE) return null
            if (RachelSDL_SeekFile(stream, offset) != offset) return null
            if (size == 0L) return []
            val bytes = ByteArray(size.toInt())
            bytes.usePinned { pinned ->
                var position = 0
                while (position < bytes.size) {
                    val count = RachelSDL_ReadFile(stream, pinned.addressOf(position), (bytes.size - position).toULong()).toLong()
                    if (count <= 0 || count > bytes.size - position) return null
                    position += count.toInt()
                }
            }
            return bytes
        }
        finally { RachelSDL_CloseFile(stream) }
    }
}
