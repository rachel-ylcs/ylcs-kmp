package org.jetbrains.skiko.window

import org.jetbrains.skiko.internal.sdl.SDL_OpenURL

internal actual fun openNativeFile(path: String): Boolean = SDL_OpenURL(path)
