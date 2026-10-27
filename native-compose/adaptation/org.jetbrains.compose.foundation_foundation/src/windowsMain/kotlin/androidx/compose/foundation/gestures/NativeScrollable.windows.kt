@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package androidx.compose.foundation.gestures

import kotlinx.cinterop.*
import platform.windows.SPI_GETWHEELSCROLLLINES
import platform.windows.SystemParametersInfoW

internal actual fun nativeScrollLineCount(): Int = memScoped {
    val lines = alloc<UIntVar>()
    if (SystemParametersInfoW(SPI_GETWHEELSCROLLLINES.toUInt(), 0u, lines.ptr, 0u) == 0) return@memScoped 3
    if (lines.value == UInt.MAX_VALUE) -1 else lines.value.coerceAtMost(Int.MAX_VALUE.toUInt()).toInt()
}
