package org.jetbrains.skiko.window

import kotlinx.cinterop.*
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.internal.sdl.*

internal class SdlWindowSession : AutoCloseable {
    private val windows = linkedMapOf<UInt, SdlWindowEntry>()
    private val wakeEventType = SDL_RegisterEvents(1).also { check(it != UInt.MAX_VALUE) { "Unable to register the Native window wake event" } }
    private var mainWindow: SdlWindowEntry? = null
    private var closing = false

    fun create(
        options: NativeWindowOptions,
        onOpen: (NativeWindowScope) -> Unit,
        onEvent: (NativeWindowEvent) -> Unit,
        onClose: () -> Unit,
        render: (Canvas, Int, Int, Long) -> Unit,
    ): NativeWindowScope {
        check(SDL_IsMainThread() && !closing && mainWindow?.scope?.shouldClose != true) { "Create windows on the active application's main thread" }
        val software = options.backend == NativeWindowBackend.SOFTWARE
        var flags = SDL_WINDOW_HIGH_PIXEL_DENSITY
        if (options.resizable) flags = flags or SDL_WINDOW_RESIZABLE
        if (!options.focusable && SDL_GetCurrentVideoDriver()?.toKString() != "wayland") flags = flags or SDL_WINDOW_NOT_FOCUSABLE
        if (!software) flags = flags or gpuWindowFlags()
        if (options.transparent) flags = flags or SDL_WINDOW_TRANSPARENT
        if (options.undecorated) flags = flags or SDL_WINDOW_BORDERLESS
        val window = checkNotNull(RachelSDL_CreateWindow(options.title, options.width, options.height, flags)) { SDL_GetError()?.toKString() ?: "SDL_CreateWindow failed" }
        var renderer: WindowRenderer? = null
        var scope: SdlWindowScope? = null
        var entry: SdlWindowEntry? = null
        try {
            var current: SdlWindowEntry? = null
            val draw: (Canvas, Int, Int) -> Unit = { canvas, width, height -> current?.draw(canvas, width, height) }
            renderer = if (software) RasterWindowRenderer(window, draw) else createGpuRenderer(window, options.transparent, draw)
            scope = SdlWindowScope(window, wakeEventType)
            entry = SdlWindowEntry(window, scope, renderer, wakeEventType, onEvent, onClose, render)
            current = entry
            windows[entry.id] = entry
            if (mainWindow == null) mainWindow = entry
            if (options.clickThrough) scope.setClickThrough(true)
            onOpen(scope)
            entry.opened()
            return scope
        }
        catch (failure: Throwable) {
            if (entry != null) {
                windows.remove(entry.id)
                if (mainWindow === entry) mainWindow = null
                try { entry.close() }
                catch (cleanup: Throwable) { if (cleanup !== failure) failure.addSuppressed(cleanup) }
            }
            else {
                val cleanup: List<() -> Unit> = [{ scope?.dispose() }, { renderer?.close() }, { RachelSDL_DestroyWindow(window) }]
                for (action in cleanup) {
                    try { action() }
                    catch (error: Throwable) { if (error !== failure) failure.addSuppressed(error) }
                }
            }
            throw failure
        }
    }

    fun run(onFrame: (Long) -> Unit) = memScoped {
        val main = checkNotNull(mainWindow)
        val event = alloc<SDL_Event>()
        while (true) {
            main.scope.rethrowCallbackFailure()
            if (main.scope.shouldClose) break
            val dirty = windows.values.any { it.dirty || it.scope.shouldClose }
            if (SDL_WaitEventTimeout(event.ptr, if (dirty) 0 else 250)) {
                do { dispatch(event) }
                while (!main.scope.shouldClose && SDL_PollEvent(event.ptr))
            }
            main.scope.rethrowCallbackFailure()
            if (main.scope.shouldClose) break
            onFrame(SDL_GetTicksNS().toLong())
            if (main.scope.shouldClose) break
            val frameStarted = SDL_GetTicks()
            val rendering = windows.values.any { it.dirty && !it.scope.shouldClose }
            for (entry in windows.values.toList()) entry.render()
            for (entry in windows.values.toList()) {
                if (entry !== main && entry.scope.shouldClose) {
                    windows.remove(entry.id)
                    entry.close()
                }
            }
            val elapsed = SDL_GetTicks() - frameStarted
            if (rendering && elapsed < 16uL) SDL_Delay((16uL - elapsed).toUInt())
        }
    }

    private fun dispatch(event: SDL_Event) {
        when (event.type) {
            SDL_EVENT_QUIT -> mainWindow?.dispatch(event)
            SDL_EVENT_SYSTEM_THEME_CHANGED, SDL_EVENT_LOCALE_CHANGED,
            SDL_EVENT_DISPLAY_ADDED, SDL_EVENT_DISPLAY_REMOVED,
            SDL_EVENT_DISPLAY_CONTENT_SCALE_CHANGED -> for (entry in windows.values.toList()) entry.dispatch(event)
            else -> windows[RachelSDL_GetEventWindowID(event.ptr)]?.dispatch(event)
        }
    }

    override fun close() {
        closing = true
        val entries = windows.values.toList().asReversed()
        windows.clear()
        var failure: Throwable? = null
        for (entry in entries) {
            try { entry.close() }
            catch (error: Throwable) {
                val previous = failure
                if (previous == null) failure = error
                else if (previous !== error) previous.addSuppressed(error)
            }
        }
        failure?.let { throw it }
    }
}
