package org.jetbrains.skiko.window

/** Owned event data; no SDL pointers escape the native event pump. Positions are physical pixels. */
sealed interface NativeWindowEvent {
    data object CloseRequested : NativeWindowEvent
    data class Focus(val focused: Boolean) : NativeWindowEvent
    data class Key(val scanCode: Int, val codePoint: Int, val down: Boolean, val repeat: Boolean, val modifiers: Int) : NativeWindowEvent
    data class Pointer(val kind: Kind, val x: Float, val y: Float, val buttons: Int, val button: Int = 0, val scrollX: Float = 0f, val scrollY: Float = 0f, val modifiers: Int = 0) : NativeWindowEvent {
        enum class Kind { MOVE, PRESS, RELEASE, SCROLL, ENTER, EXIT }
    }
    data class TextInput(val text: String) : NativeWindowEvent
    /** [start] and [length] count Unicode code points in [text], as reported by SDL. */
    data class TextEditing(val text: String, val start: Int, val length: Int) : NativeWindowEvent
    data class Drop(val text: String, val isFile: Boolean, val x: Float, val y: Float) : NativeWindowEvent
    data object DropStarted : NativeWindowEvent
    data object DropCompleted : NativeWindowEvent
}
