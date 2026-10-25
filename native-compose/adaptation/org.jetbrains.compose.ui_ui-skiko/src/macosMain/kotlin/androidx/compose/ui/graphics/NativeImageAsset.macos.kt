package androidx.compose.ui.graphics

internal actual fun ByteArray.putBytesInto(array: IntArray, offset: Int, length: Int) = copySkikoPixelBytes(this, array, offset, length)
