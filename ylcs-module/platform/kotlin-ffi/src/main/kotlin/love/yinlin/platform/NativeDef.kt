package love.yinlin.platform

import love.yinlin.platform.Native.Type
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
    val Boolean: Type = ValueLayout.JAVA_BOOLEAN
    val Byte: Type = ValueLayout.JAVA_BYTE
    val Char: Type = ValueLayout.JAVA_CHAR
    val Int: Type = ValueLayout.JAVA_INT
    val Short: Type = ValueLayout.JAVA_SHORT
    val Long: Type = ValueLayout.JAVA_LONG
    val Float: Type = ValueLayout.JAVA_FLOAT
    val Double: Type = ValueLayout.JAVA_DOUBLE
    val Pointer: Type = ValueLayout.ADDRESS


    val string: Type = Pointer
    val wstring: Type = Pointer
}

val Native.Pointer.isNull get() = this == Native.NULL
val Native.Pointer.isNotNull get() = this != Native.NULL

fun Arena.aString(str: String): Native.Pointer = allocateFrom(str, StandardCharsets.UTF_8)
fun Arena.wString(str: String): Native.Pointer = allocateFrom(str, StandardCharsets.UTF_16LE)