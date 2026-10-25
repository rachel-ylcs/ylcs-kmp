@file:OptIn(ExperimentalNativeApi::class)

package love.yinlin.nativecompose.ui

import androidx.compose.ui.graphics.SkikoColorFilter
import androidx.compose.ui.graphics.colorMatrixFromSkiko
import androidx.compose.ui.graphics.putBytesInto
import androidx.compose.ui.text.WeakKeysCache
import androidx.compose.ui.text.platform.Platform
import androidx.compose.ui.text.platform.currentPlatform
import org.jetbrains.skia.ColorFilter
import org.jetbrains.skia.ColorMatrix
import org.jetbrains.skia.impl.use
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.OsFamily
import kotlin.native.Platform as NativePlatform
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NativeSkikoBackendTest {
    @Test
    fun platformActualMatchesTheHost() {
        val expected = when (NativePlatform.osFamily) {
            OsFamily.WINDOWS -> Platform.Windows
            OsFamily.LINUX -> Platform.Linux
            OsFamily.MACOSX -> Platform.MacOS
            else -> error("Unsupported Native desktop host")
        }
        assertEquals(expected, currentPlatform())
    }

    @Test
    fun weakKeyCacheRetainsLiveKeysAndSeparatesDistinctKeys() {
        val key = Any()
        val cache = WeakKeysCache<Any, String>()
        var loads = 0
        assertEquals("first", cache.getOrPut(key) {
            loads++
            "first"
        })
        assertEquals("first", cache.getOrPut(key) {
            loads++
            "second"
        })
        assertEquals(1, loads)
        assertEquals("other", cache.getOrPut(Any()) {
            loads++
            "other"
        })
        assertEquals(2, loads)
    }

    @Test
    fun pixelCopyPreservesLittleEndianArgbAndDestinationOffset() {
        val source: ByteArray = [4, 3, 2, 1, 8, 7, 6, 5]
        val destination: IntArray = [-1, -1, -1, -1]
        source.putBytesInto(destination, 1, 2)
        val expected: IntArray = [-1, 0x01020304, 0x05060708, -1]
        assertContentEquals(expected, destination)
        ByteArray(0).putBytesInto(IntArray(0), 0, 0)
    }

    @Test
    fun pixelCopyRejectsInvalidInputBeforeEnteringNativeMemory() {
        assertFailsWith<IllegalArgumentException> { ByteArray(0).putBytesInto(IntArray(1), 0, 1) }
        assertFailsWith<IllegalArgumentException> { ByteArray(4).putBytesInto(IntArray(1), -1, 1) }
        assertFailsWith<IllegalArgumentException> { ByteArray(4).putBytesInto(IntArray(1), 0, Int.MAX_VALUE) }
    }

    @Test
    fun skiaMatrixExtractionRetainsScaleAndRestoresComposeTranslationUnits() {
        val values = FloatArray(20)
        values[0] = .5f
        values[4] = .25f
        values[6] = 1f
        values[12] = 1f
        values[18] = .75f
        ColorFilter.makeMatrix(ColorMatrix(values)).use { filter ->
            val matrix = colorMatrixFromSkiko(SkikoColorFilter(filter))
            assertEquals(.5f, matrix[0, 0], .00001f)
            assertEquals(63.75f, matrix[0, 4], .0001f)
            assertEquals(.75f, matrix[3, 3], .00001f)
        }
    }
}
