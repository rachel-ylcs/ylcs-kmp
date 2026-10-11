package love.yinlin.foundation.compression

import love.yinlin.encoding.convert
import love.yinlin.extension.cleaning
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.Inflater
import kotlin.random.Random
import kotlin.test.*

class TestCompressionInterop {
    private val sources: List<ByteArray> = [
        [],
        ByteArray(256) { it.toByte() },
        Random(1950).nextBytes(140000),
        "The quick brown fox jumps over the lazy dog. ".repeat(4000).convert(),
    ]

    private fun deflateWithJdk(source: ByteArray, level: Int, raw: Boolean): ByteArray {
        val deflater = Deflater(level, raw)
        cleaning(deflater::end) {
            deflater.setInput(source)
            deflater.finish()
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            while (!deflater.finished()) {
                val count = deflater.deflate(buffer)
                assertTrue(count > 0 || deflater.finished())
                output.write(buffer, 0, count)
            }
            return output.toByteArray()
        }
    }

    private fun inflateWithJdk(encoded: ByteArray, raw: Boolean): ByteArray {
        val inflater = Inflater(raw)
        cleaning(inflater::end) {
            inflater.setInput(encoded)
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                assertTrue(count > 0 || inflater.finished())
                output.write(buffer, 0, count)
            }
            assertEquals(0, inflater.remaining)
            return output.toByteArray()
        }
    }

    @Test
    fun rawDeflateIsBidirectionallyCompatibleWithJdk() {
        for (source in sources) {
            val codec = Deflate()
            assertContentEquals(source, inflateWithJdk(codec.compress(source), true))
            for (level in [Deflater.NO_COMPRESSION, Deflater.BEST_SPEED, Deflater.BEST_COMPRESSION]) {
                assertDecoded(codec, deflateWithJdk(source, level, true), source)
            }
        }
    }

    @Test
    fun zlibIsBidirectionallyCompatibleWithJdk() {
        for (source in sources) {
            val codec = Zlib()
            assertContentEquals(source, inflateWithJdk(codec.compress(source), false))
            for (level in [Deflater.NO_COMPRESSION, Deflater.BEST_SPEED, Deflater.BEST_COMPRESSION]) {
                assertDecoded(codec, deflateWithJdk(source, level, false), source)
            }
        }
    }

    @Test
    fun gzipIsBidirectionallyCompatibleWithJdkIncludingConcatenatedMembers() {
        for (source in sources) {
            val codec = Gzip()
            val encoded = codec.compress(source)
            GZIPInputStream(ByteArrayInputStream(encoded)).use { assertContentEquals(source, it.readBytes()) }
            val output = ByteArrayOutputStream()
            GZIPOutputStream(output).use { it.write(source) }
            assertDecoded(codec, output.toByteArray(), source)
            GZIPInputStream(ByteArrayInputStream(encoded + encoded)).use { assertContentEquals(source + source, it.readBytes()) }
        }
    }
}
