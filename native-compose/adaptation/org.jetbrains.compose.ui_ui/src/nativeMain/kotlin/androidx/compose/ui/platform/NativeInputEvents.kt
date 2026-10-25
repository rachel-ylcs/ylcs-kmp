package androidx.compose.ui.platform

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import org.jetbrains.skiko.window.NativeWindowEvent

/** SDL modifiers are copied as integer flags, keeping cinterop out of the UI module. */
internal fun nativeKeyboardModifiers(flags: Int): PointerKeyboardModifiers = PointerKeyboardModifiers(
    isCtrlPressed = flags and 0x00C0 != 0,
    isMetaPressed = flags and 0x0C00 != 0,
    isAltPressed = flags and 0x0300 != 0,
    isShiftPressed = flags and 0x0003 != 0,
    isAltGraphPressed = flags and 0x4000 != 0,
    isCapsLockOn = flags and 0x2000 != 0,
    isScrollLockOn = flags and 0x8000 != 0,
    isNumLockOn = flags and 0x1000 != 0
)

internal fun nativeKey(event: NativeWindowEvent.Key): Key {
    val isNavigation = event.modifiers and 0x1000 == 0 && (event.scanCode in 89 .. 99 && event.scanCode != 93)
    return Key(event.scanCode.toLong() + if (isNavigation) 65536 else 0)
}

/** SDL's IME offsets count Unicode scalar values; Compose editing commands use UTF-16 offsets. */
internal fun nativeUtf16Offset(text: String, codePoints: Int): Int {
    var offset = 0
    var remaining = codePoints.coerceAtLeast(0)
    while (offset < text.length && remaining > 0) {
        val high = text[offset++]
        if (high.isHighSurrogate() && offset < text.length && text[offset].isLowSurrogate()) offset++
        remaining--
    }
    return offset
}
