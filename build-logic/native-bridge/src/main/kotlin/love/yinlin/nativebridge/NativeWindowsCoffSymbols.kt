package love.yinlin.nativebridge

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Keeps Kotlin/Native's COFF symbols and DWARF section names across Windows resource updates. */
internal class NativeWindowsCoffSymbols private constructor(
    private val count: Int,
    private val sections: List<String>,
    private val table: ByteArray,
) {
    fun restore(executable: File) {
        RandomAccessFile(executable, "rw").use { file ->
            val header = readHeader(file)
            val indices = sections.map { name ->
                val index = header.sections.indexOf(name)
                require(index >= 0) { "Windows resource update removed COFF section $name" }
                index + 1
            }
            val restored = table.copyOf()
            val symbols = ByteBuffer.wrap(restored).order(ByteOrder.LITTLE_ENDIAN)
            var index = 0
            while (index < count) {
                val offset = index * SYMBOL_SIZE
                val section = symbols.getShort(offset + 12).toInt()
                val auxiliary = restored[offset + 17].toInt() and 0xff
                require(index.toLong() + auxiliary < count) { "Invalid COFF auxiliary symbol count" }
                if (section > 0) {
                    require(section <= indices.size) { "Invalid COFF symbol section $section" }
                    symbols.putShort(offset + 12, indices[section - 1].toShort())
                    // Section-definition auxiliaries can refer to another section (associative COMDAT).
                    if (auxiliary > 0 && restored[offset + 16].toInt() == 3 && symbols.getShort(offset + 14).toInt() == 0 && symbols.getInt(offset + 8) == 0) {
                        val auxiliaryOffset = offset + SYMBOL_SIZE
                        if (restored[auxiliaryOffset + 14].toInt() == 5) {
                            val associated = symbols.getShort(auxiliaryOffset + 12).toInt() and 0xffff
                            require(associated in 1 .. indices.size) { "Invalid COFF associated section $associated" }
                            symbols.putShort(auxiliaryOffset + 12, indices[associated - 1].toShort())
                        }
                    }
                }
                index += auxiliary + 1
            }

            val offset = (file.length() + 3) and -4L
            require(offset <= 0xffffffffL) { "COFF symbol table exceeds the PE file offset range" }
            file.seek(offset)
            file.write(restored)
            file.seek(header.offset + 12)
            file.writeInt(Integer.reverseBytes(offset.toInt()))
            file.writeInt(Integer.reverseBytes(count))
            // Resource updates and the restored table invalidate the optional image checksum.
            file.seek(header.offset + 24 + 64)
            file.writeInt(0)
        }
    }

    companion object {
        private const val SYMBOL_SIZE = 18

        fun read(executable: File): NativeWindowsCoffSymbols? = RandomAccessFile(executable, "r").use { file ->
            val header = readHeader(file)
            if (header.symbolOffset == 0L && header.symbolCount == 0L) return@use null
            val stringOffset = header.symbolOffset + header.symbolCount * SYMBOL_SIZE
            require(header.symbolOffset > 0 && stringOffset + 4 <= file.length()) { "COFF symbol table is outside the executable" }
            file.seek(stringOffset)
            val stringSize = Integer.toUnsignedLong(Integer.reverseBytes(file.readInt()))
            val size = header.symbolCount * SYMBOL_SIZE + stringSize
            require(stringSize >= 4 && size <= Int.MAX_VALUE && header.symbolOffset + size <= file.length()) { "Invalid COFF string table size" }
            val table = ByteArray(size.toInt())
            file.seek(header.symbolOffset)
            file.readFully(table)
            NativeWindowsCoffSymbols(header.symbolCount.toInt(), header.sections, table)
        }

        private data class Header(val offset: Long, val sections: List<String>, val symbolOffset: Long, val symbolCount: Long)

        private fun readHeader(file: RandomAccessFile): Header {
            require(file.length() >= 64 && file.readUnsignedShort() == 0x4d5a) { "Invalid DOS executable header" }
            file.seek(0x3c)
            val offset = Integer.toUnsignedLong(Integer.reverseBytes(file.readInt()))
            require(offset >= 64 && offset + 24 <= file.length()) { "Invalid PE header offset" }
            file.seek(offset)
            val bytes = ByteArray(24)
            file.readFully(bytes)
            val header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            require(header.int == 0x4550) { "Invalid PE signature" }
            val count = header.getShort(6).toInt() and 0xffff
            val optionalSize = header.getShort(20).toInt() and 0xffff
            require(count in 1 .. Short.MAX_VALUE && optionalSize >= 68 && offset + 24 + optionalSize + count * 40 <= file.length()) { "Invalid PE section table" }
            val sections = List(count) { index ->
                file.seek(offset + 24 + optionalSize + index * 40)
                val name = ByteArray(8)
                file.readFully(name)
                name.toString(Charsets.ISO_8859_1).trimEnd('\u0000')
            }
            return Header(offset, sections, Integer.toUnsignedLong(header.getInt(12)), Integer.toUnsignedLong(header.getInt(16)))
        }
    }
}
