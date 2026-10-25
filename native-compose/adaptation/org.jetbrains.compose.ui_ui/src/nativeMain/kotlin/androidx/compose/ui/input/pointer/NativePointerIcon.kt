package androidx.compose.ui.input.pointer

import androidx.compose.runtime.Stable
import org.jetbrains.skiko.window.NativeWindowCursor

@Stable
internal data class NativePointerIcon(val cursor: NativeWindowCursor) : PointerIcon

internal actual val pointerIconDefault: PointerIcon = NativePointerIcon(NativeWindowCursor.DEFAULT)
internal actual val pointerIconCrosshair: PointerIcon = NativePointerIcon(NativeWindowCursor.CROSSHAIR)
internal actual val pointerIconText: PointerIcon = NativePointerIcon(NativeWindowCursor.TEXT)
internal actual val pointerIconHand: PointerIcon = NativePointerIcon(NativeWindowCursor.HAND)
