package love.yinlin.platform.ffi

import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

typealias Address = MemorySegment
typealias Layout = MemoryLayout

object NativeType {
    val Boolean: Layout = ValueLayout.JAVA_BOOLEAN
    val Byte: Layout = ValueLayout.JAVA_BYTE
    val Char: Layout = ValueLayout.JAVA_CHAR
    val Int: Layout = ValueLayout.JAVA_INT
    val Short: Layout = ValueLayout.JAVA_SHORT
    val Long: Layout = ValueLayout.JAVA_LONG
    val Float: Layout = ValueLayout.JAVA_FLOAT
    val Double: Layout = ValueLayout.JAVA_DOUBLE
    val Pointer: Layout = ValueLayout.ADDRESS

    val String: Layout = Pointer
    val WString: Layout = Pointer
}