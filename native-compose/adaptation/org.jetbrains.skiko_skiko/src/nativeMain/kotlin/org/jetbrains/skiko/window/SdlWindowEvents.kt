package org.jetbrains.skiko.window

import kotlinx.cinterop.*
import org.jetbrains.skiko.internal.sdl.*
import org.jetbrains.skiko.internal.sdl.SDL_MouseWheelDirection.SDL_MOUSEWHEEL_FLIPPED

internal class SdlWindowEvents(private val window: COpaquePointer, private val send: (NativeWindowEvent) -> Unit) {
    private var x = 0f
    private var y = 0f
    private var buttons = 0

    fun dispatch(event: SDL_Event) {
        val scale = RachelSDL_GetWindowPixelDensity(window).takeIf { it.isFinite() && it > 0f } ?: 1f
        when (event.type) {
            SDL_EVENT_QUIT, SDL_EVENT_WINDOW_CLOSE_REQUESTED -> send(NativeWindowEvent.CloseRequested)
            SDL_EVENT_WINDOW_FOCUS_GAINED -> send(NativeWindowEvent.Focus(true))
            SDL_EVENT_WINDOW_FOCUS_LOST -> {
                buttons = 0
                send(NativeWindowEvent.Focus(false))
            }
            SDL_EVENT_KEY_DOWN, SDL_EVENT_KEY_UP -> {
                val code = SDL_GetKeyFromScancode(event.key.scancode, event.key.mod, true).toLong()
                val codePoint = if (code in 0 .. 0x10FFFF && code !in 0xD800 .. 0xDFFF) code.toInt() else 0
                send(NativeWindowEvent.Key(event.key.scancode.toInt(), codePoint, event.key.down, event.key.repeat, event.key.mod.toInt()))
            }
            SDL_EVENT_MOUSE_MOTION -> {
                x = event.motion.x * scale
                y = event.motion.y * scale
                buttons = event.motion.state.toInt()
                pointer(NativeWindowEvent.Pointer.Kind.MOVE)
            }
            SDL_EVENT_MOUSE_BUTTON_DOWN, SDL_EVENT_MOUSE_BUTTON_UP -> {
                x = event.button.x * scale
                y = event.button.y * scale
                val button = event.button.button.toInt()
                if (button in 1 .. 31) {
                    val mask = 1 shl (button - 1)
                    buttons = if (event.button.down) buttons or mask else buttons and mask.inv()
                }
                pointer(if (event.button.down) NativeWindowEvent.Pointer.Kind.PRESS else NativeWindowEvent.Pointer.Kind.RELEASE, button)
            }
            SDL_EVENT_MOUSE_WHEEL -> {
                x = event.wheel.mouse_x * scale
                y = event.wheel.mouse_y * scale
                val direction = if (event.wheel.direction == SDL_MOUSEWHEEL_FLIPPED) 1f else -1f
                send(NativeWindowEvent.Pointer(NativeWindowEvent.Pointer.Kind.SCROLL, x, y, buttons,
                    scrollX = event.wheel.x * direction, scrollY = event.wheel.y * direction, modifiers = SDL_GetModState().toInt()))
            }
            SDL_EVENT_WINDOW_MOUSE_ENTER -> pointer(NativeWindowEvent.Pointer.Kind.ENTER)
            SDL_EVENT_WINDOW_MOUSE_LEAVE -> pointer(NativeWindowEvent.Pointer.Kind.EXIT)
            SDL_EVENT_TEXT_INPUT -> event.text.text?.toKString()?.let { send(NativeWindowEvent.TextInput(it)) }
            SDL_EVENT_TEXT_EDITING -> send(NativeWindowEvent.TextEditing(event.edit.text?.toKString() ?: "", event.edit.start, event.edit.length))
            SDL_EVENT_DROP_FILE, SDL_EVENT_DROP_TEXT -> event.drop.data?.toKString()?.let {
                send(NativeWindowEvent.Drop(it, event.type == SDL_EVENT_DROP_FILE, event.drop.x * scale, event.drop.y * scale))
            }
            SDL_EVENT_DROP_BEGIN -> send(NativeWindowEvent.DropStarted)
            SDL_EVENT_DROP_COMPLETE -> send(NativeWindowEvent.DropCompleted)
        }
    }

    private fun pointer(kind: NativeWindowEvent.Pointer.Kind, button: Int = 0) {
        send(NativeWindowEvent.Pointer(kind, x, y, buttons, button, modifiers = SDL_GetModState().toInt()))
    }
}
