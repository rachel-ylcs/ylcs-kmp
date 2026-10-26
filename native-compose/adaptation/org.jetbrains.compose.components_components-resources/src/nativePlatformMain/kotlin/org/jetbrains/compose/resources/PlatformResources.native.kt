package org.jetbrains.compose.resources

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import org.jetbrains.skiko.internal.sdl.*
import org.jetbrains.skiko.internal.sdl.SDL_IOWhence.SDL_IO_SEEK_SET
import org.jetbrains.skiko.internal.sdl.SDL_PathType.SDL_PATHTYPE_FILE

internal actual fun resourceBaseDirectory(): String = checkNotNull(SDL_GetBasePath()?.toKString()) { "Cannot locate the Native application's resource directory" }

internal actual fun resourceDisplayScale(): Float {
    // Composable lookups use LocalDensity; background/non-window lookups use the default density.
    val display = if (SDL_IsMainThread() && SDL_WasInit(SDL_INIT_VIDEO) != 0u) SDL_GetPrimaryDisplay() else 0u
    return if (display != 0u) SDL_GetDisplayContentScale(display).takeIf { it.isFinite() && it > 0f } ?: 1f else 1f
}

internal actual fun resourceFileExists(path: String): Boolean = memScoped {
    val info = alloc<SDL_PathInfo>()
    SDL_GetPathInfo(path, info.ptr) && info.type == SDL_PATHTYPE_FILE
}

internal actual fun readResourceFile(path: String, offset: Long, requestedSize: Long?): ByteArray {
    // SDL uses UTF-8 paths and 64-bit offsets on every desktop target, including Windows.
    val stream = SDL_IOFromFile(path, "rb") ?: throw MissingResourceException(path)
    try {
        val length = SDL_GetIOSize(stream)
        if (length < 0 || offset > length) throw MissingResourceException(path, "Cannot read the requested file range")
        val available = length - offset
        val size = requestedSize ?: available
        if (size > available || size > Int.MAX_VALUE) throw MissingResourceException(path, "Resource range exceeds its file or ByteArray capacity")
        if (SDL_SeekIO(stream, offset, SDL_IO_SEEK_SET) != offset) throw MissingResourceException(path, "Cannot seek to the resource range")
        if (size == 0L) return []
        val bytes = ByteArray(size.toInt())
        bytes.usePinned { pinned ->
            var position = 0
            while (position < bytes.size) {
                val count = SDL_ReadIO(stream, pinned.addressOf(position), (bytes.size - position).toULong()).toLong()
                if (count <= 0 || count > bytes.size - position) throw MissingResourceException(path, "Unexpected end of resource file")
                position += count.toInt()
            }
        }
        return bytes
    }
    finally { SDL_CloseIO(stream) }
}
