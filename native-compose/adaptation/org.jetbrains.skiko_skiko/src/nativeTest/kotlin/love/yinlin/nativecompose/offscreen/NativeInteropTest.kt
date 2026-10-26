@file:OptIn(org.jetbrains.skiko.InternalSkikoApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package love.yinlin.nativecompose.offscreen

import org.jetbrains.skia.ManagedString
import org.jetbrains.skia.impl.*
import org.jetbrains.skia.shaper.IcuBidiRunIterator
import org.jetbrains.skia.shaper.TrivialBidiRunIterator
import kotlin.test.*

class NativeInteropTest {
    @Test
    fun borrowedShapingTextStaysReadableAndIsNotClosedByItsIterator() {
        ManagedString("A😀中文").use { text ->
            IcuBidiRunIterator(text, false, 0).use { iterator ->
                var end = 0
                while (iterator.hasNext()) end = iterator.next().end
                assertEquals(5, end)
                assertFailsWith<NoSuchElementException> { iterator.next() }
                assertFailsWith<UnsupportedOperationException> { iterator.remove() }
            }
            assertFalse(text.isClosed)
        }
        val empty = TrivialBidiRunIterator("", 0)
        assertFalse(empty.hasNext())
        assertFailsWith<NoSuchElementException> { empty.next() }
    }

    @Test
    fun pointerBuffersRoundTripWithExplicitOrKnownLengths(): Unit = interopScope {
        val array = NativePointerArray(2)
        array[0] = Native.NullPointer + 42L
        array[1] = Native.NullPointer + 84L
        val raw = toInterop(array)
        assertEquals(array[0], raw.fromInteropNativePointerArray()[0])
        assertEquals(array[1], raw.fromInteropNativePointerArray(2)[1])
        assertEquals(0, Native.NullPointer.fromInteropNativePointerArray().size)
        assertFailsWith<IllegalArgumentException> { Native.NullPointer.fromInteropNativePointerArray(1) }
        assertFailsWith<IllegalArgumentException> { raw.fromInteropNativePointerArray(-1) }
    }

    @Test
    fun arrayDecoderFailureAlwaysReleasesItsNativeBuffer() = interopScope {
        var disposed = false
        val decoder = object : ArrayInteropDecoder<Int> {
            override fun getArraySize(array: InteropPointer): Int = 1
            override fun getArrayElement(array: InteropPointer, index: Int): Int = error("decode failure")
            override fun disposeArray(array: InteropPointer) { disposed = true }
        }
        assertFailsWith<IllegalStateException> { Native.NullPointer.fromInterop(decoder) }
        assertTrue(disposed)
    }
}
