@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.jetbrains.skiko.window

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.toKString
import org.jetbrains.skiko.internal.sdl.*

/** Keeps SDL bindings inside the shared Native fragment, including IDE imports on other hosts. */
internal object SdlMetalWindow {
    val flags: ULong get() = SDL_WINDOW_METAL

    fun createView(window: COpaquePointer): COpaquePointer = checkNotNull(RachelSDL_CreateMetalView(window)) { SDL_GetError()?.toKString() ?: "Cannot create SDL Metal view" }

    fun layer(view: COpaquePointer): COpaquePointer = checkNotNull(RachelSDL_GetMetalLayer(view)) { "SDL did not expose a CAMetalLayer" }

    fun destroyView(view: COpaquePointer) { RachelSDL_DestroyMetalView(view) }

    fun requestRender(window: COpaquePointer) { RachelSDL_RequestRender(window) }
}
