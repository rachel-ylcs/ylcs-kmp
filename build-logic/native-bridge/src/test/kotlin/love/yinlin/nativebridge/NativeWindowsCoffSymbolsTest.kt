package love.yinlin.nativebridge

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.*

class NativeWindowsCoffSymbolsTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test
    fun resourceUpdatesKeepSymbolsLongSectionNamesAndSectionAssociations() {
        val file = temporary.newFile("app.exe")
        val original = executable(listOf(".text", "/4"))
        file.writeBytes(original)
        val saved = assertNotNull(NativeWindowsCoffSymbols.read(file))

        // Reproduce EndUpdateResourceW: insert .rsrc, drop the table, retain a stale header.
        val updated = executable(listOf(".text", ".rsrc", "/4")).copyOf(1024)
        ByteBuffer.wrap(updated).order(ByteOrder.LITTLE_ENDIAN).putInt(0x80 + 12, 768)
        file.writeBytes(updated)
        saved.restore(file)

        val bytes = file.readBytes()
        val restored = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val offset = restored.getInt(0x80 + 12)
        assertTrue(offset >= updated.size)
        assertEquals(5, restored.getInt(0x80 + 16))
        assertEquals(0, restored.getInt(0x80 + 24 + 64))
        assertEquals(1, restored.getShort(offset + 12).toInt())
        assertEquals(3, restored.getShort(offset + 18 + 12).toInt())
        assertEquals(3, restored.getShort(offset + 36 + 12).toInt())
        assertEquals(-2, restored.getShort(offset + 54 + 12).toInt())
        assertEquals("fixture.kt", bytes.copyOfRange(offset + 72, offset + 82).toString(Charsets.UTF_8))
        assertContentEquals(original.copyOfRange(1024 + 5 * 18, original.size), bytes.copyOfRange(offset + 5 * 18, bytes.size))
        assertNotNull(NativeWindowsCoffSymbols.read(file))
    }

    @Test
    fun executablesWithoutSymbolsNeedNoRestoration() {
        val file = temporary.newFile("stripped.exe")
        val bytes = executable(listOf(".text"))
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(0x80 + 12, 0).putInt(0x80 + 16, 0)
        file.writeBytes(bytes.copyOf(1024))
        assertNull(NativeWindowsCoffSymbols.read(file))
    }

    @Test
    fun malformedTablesAndMissingSectionsFailBeforePackagingSucceeds() {
        val file = temporary.newFile("invalid.exe")
        val bytes = executable(listOf(".text", "/4"))
        file.writeBytes(bytes.copyOf(bytes.size - 1))
        assertFailsWith<IllegalArgumentException> { NativeWindowsCoffSymbols.read(file) }
        file.writeBytes(bytes)
        val saved = assertNotNull(NativeWindowsCoffSymbols.read(file))
        file.writeBytes(executable(listOf(".text", ".rsrc")))
        assertFailsWith<IllegalArgumentException> { saved.restore(file) }
    }

    private fun executable(sections: List<String>): ByteArray {
        val strings = ".debug_info\u0000".toByteArray(Charsets.UTF_8)
        val bytes = ByteArray(1024 + 5 * 18 + 4 + strings.size)
        val output = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        output.putShort(0, 0x5a4d).putInt(0x3c, 0x80)
        output.putInt(0x80, 0x4550).putShort(0x80 + 4, 0x8664.toShort()).putShort(0x80 + 6, sections.size.toShort())
        output.putInt(0x80 + 12, 1024).putInt(0x80 + 16, 5).putShort(0x80 + 20, 240)
        output.putShort(0x80 + 24, 0x20b).putInt(0x80 + 24 + 64, 42)
        for ((index, name) in sections.withIndex()) {
            output.position(0x80 + 24 + 240 + index * 40)
            output.put(name.toByteArray(Charsets.ISO_8859_1))
        }
        output.position(1024)
        output.put(".text".toByteArray()).position(1024 + 12)
        output.putShort(1).putShort(0).put(3).put(1)
        output.putShort(1024 + 18 + 12, 2).put(1024 + 18 + 14, 5)
        output.putInt(1024 + 36 + 4, 4).putShort(1024 + 36 + 12, 2).putShort(1024 + 36 + 14, 0x20).put(1024 + 36 + 16, 2)
        output.position(1024 + 54)
        output.put(".file".toByteArray()).position(1024 + 54 + 12)
        output.putShort(-2).putShort(0).put(103).put(1)
        output.position(1024 + 72)
        output.put("fixture.kt".toByteArray())
        output.position(1024 + 5 * 18)
        output.putInt(strings.size + 4).put(strings)
        return bytes
    }
}
