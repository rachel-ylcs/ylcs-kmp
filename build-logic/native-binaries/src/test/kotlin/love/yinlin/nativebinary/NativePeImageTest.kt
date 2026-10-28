package love.yinlin.nativebinary

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertFailsWith

class NativePeImageTest {
    @Test
    fun removingOnlyTheCoffTablePreservesExecutionAndUnwinding() {
        val original = fixture()
        val stripped = stripped(original)
        NativePeImage.verifyStripped(original, stripped)
        val headers = ByteBuffer.wrap(stripped).order(ByteOrder.LITTLE_ENDIAN)
        val textFlags = 0x98 + 240 + 36
        headers.putInt(textFlags, headers.getInt(textFlags) or 0x00500000)
        NativePeImage.verifyStripped(original, stripped)
    }

    @Test
    fun staleSymbolsAndModifiedRuntimeDataAreRejected() {
        val original = fixture()
        val stripped = stripped(original)
        for (offset in listOf(0x80 + 12, 0x80 + 16, 0x80 + 24 + 16, 0x80 + 24 + 112 + 3 * 8, 1024, 1536, 2048)) {
            val damaged = stripped.copyOf()
            damaged[offset] = (damaged[offset].toInt() xor 1).toByte()
            assertFailsWith<IllegalArgumentException>("Accepted damage at $offset") { NativePeImage.verifyStripped(original, damaged) }
        }
    }

    private fun stripped(original: ByteArray): ByteArray = original.copyOf(2560).also {
        ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).putInt(0x80 + 12, 0).putInt(0x80 + 16, 0)
    }

    private fun fixture(): ByteArray {
        val bytes = ByteArray(2582) { (it % 251).toByte() }
        bytes.fill(0, 0, 1024)
        val output = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        output.putShort(0, 0x5a4d).putInt(0x3c, 0x80)
        output.putInt(0x80, 0x4550).putShort(0x84, 0x8664.toShort()).putShort(0x86, 3)
        output.putInt(0x8c, 2560).putInt(0x90, 1).putShort(0x94, 240)
        output.putShort(0x98, 0x20b)
        for ((index, name) in listOf(".text", ".pdata", ".reloc").withIndex()) {
            val header = 0x98 + 240 + index * 40
            output.position(header)
            output.put(name.toByteArray())
            output.putInt(header + 8, 512).putInt(header + 12, (index + 1) * 4096)
            output.putInt(header + 16, 512).putInt(header + 20, 1024 + index * 512)
            output.putInt(header + 36, if (index == 2) 0x42000040 else 0x60000020)
        }
        return bytes
    }
}
