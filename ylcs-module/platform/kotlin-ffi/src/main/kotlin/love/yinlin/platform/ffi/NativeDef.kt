package love.yinlin.platform.ffi

import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.nio.charset.StandardCharsets

object Native {
    typealias Pointer = MemorySegment
    typealias Type = MemoryLayout

    val NULL: Pointer = Pointer.NULL
}

object NativeType {
    val Boolean: Native.Type = ValueLayout.JAVA_BOOLEAN
    val Byte: Native.Type = ValueLayout.JAVA_BYTE
    val Char: Native.Type = ValueLayout.JAVA_CHAR
    val Int: Native.Type = ValueLayout.JAVA_INT
    val Short: Native.Type = ValueLayout.JAVA_SHORT
    val Long: Native.Type = ValueLayout.JAVA_LONG
    val Float: Native.Type = ValueLayout.JAVA_FLOAT
    val Double: Native.Type = ValueLayout.JAVA_DOUBLE
    val Pointer: Native.Type = ValueLayout.ADDRESS


    val string: Native.Type = Pointer
    val wstring: Native.Type = Pointer
}

val Native.Pointer.isNull get() = this == Native.NULL
val Native.Pointer.isNotNull get() = this != Native.NULL

fun Arena.aString(str: String): Native.Pointer = allocateFrom(str, StandardCharsets.UTF_8)
fun Arena.wString(str: String): Native.Pointer = allocateFrom(str, StandardCharsets.UTF_16LE)