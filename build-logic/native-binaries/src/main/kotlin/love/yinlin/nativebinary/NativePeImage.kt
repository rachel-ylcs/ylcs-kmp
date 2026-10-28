package love.yinlin.nativebinary

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Validate PE loader data, especially x64 unwind tables, before accepting a stripped Windows binary. */
internal class NativePeImage(private val bytes: ByteArray) {
    private val input = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    private val pe: Int
    private val optional: Int
    private val sections: List<Section>

    init {
        require(bytes.size >= 64 && input.getShort(0).toInt() == 0x5a4d) { "Not a PE image" }
        pe = input.getInt(0x3c)
        require(pe >= 64 && pe.toLong() + 24 <= bytes.size && input.getInt(pe) == 0x4550) { "Invalid PE header" }
        optional = pe + 24
        val optionalSize = input.getShort(pe + 20).toInt() and 0xffff
        require(optionalSize >= 240 && optional.toLong() + optionalSize <= bytes.size && input.getShort(optional).toInt() == 0x20b) { "Expected a PE32+ image" }
        val count = input.getShort(pe + 6).toInt() and 0xffff
        require(optional.toLong() + optionalSize + count * 40L <= bytes.size) { "Invalid PE sections" }
        sections = (0 ..< count).map { index ->
            val header = optional + optionalSize + index * 40
            val size = input.getInt(header + 16)
            val raw = input.getInt(header + 20)
            require(input.getInt(header + 8) >= 0 && size >= 0 && raw >= 0 && raw.toLong() + size <= bytes.size) { "Invalid PE section bounds" }
            Section(bytes.copyOfRange(header, header + 8).toString(Charsets.ISO_8859_1).trimEnd('\u0000'), input.getInt(header + 8), input.getInt(header + 12), size, raw, input.getInt(header + 36))
        }
    }

    private data class Section(val name: String, val virtualSize: Int, val address: Int, val size: Int, val raw: Int, val flags: Int)

    private fun loaded(): List<Section> = sections.filter { it.name == ".reloc" || it.flags and 0xe0000000.toInt() != 0 && it.flags and 0x02000000 == 0 }

    companion object {
        fun verifyStripped(original: ByteArray, stripped: ByteArray) {
            val before = NativePeImage(original)
            val after = NativePeImage(stripped)
            require(after.input.getInt(after.pe + 12) == 0 && after.input.getInt(after.pe + 16) == 0) { "Stripped PE retains an invalid COFF symbol table" }
            require(before.input.getShort(before.pe + 4) == after.input.getShort(after.pe + 4)) { "PE machine changed" }
            for (offset in listOf(16, 24, 32, 36, 56, 68, 70)) {
                val size = when (offset) { 24 -> 8; 68, 70 -> 2; else -> 4 }
                require(original.copyOfRange(before.optional + offset, before.optional + offset + size).contentEquals(stripped.copyOfRange(after.optional + offset, after.optional + offset + size))) { "PE execution header changed at $offset" }
            }
            // Preserve import/export, exception, relocation, TLS and load-configuration directory addresses.
            for (index in listOf(0, 1, 2, 3, 5, 9, 10, 12, 13)) {
                val offset = 112 + index * 8
                require(original.copyOfRange(before.optional + offset, before.optional + offset + 8).contentEquals(stripped.copyOfRange(after.optional + offset, after.optional + offset + 8))) { "PE loader directory $index changed" }
            }
            val loaded = before.loaded()
            require(loaded.map { it.name } == after.loaded().map { it.name }) { "Loaded PE sections changed" }
            for ((left, right) in loaded.zip(after.loaded())) {
                // BFD may populate IMAGE_SCN_ALIGN_* bits. PE loaders ignore these object-file-only bits.
                val runtimeFlags = 0x00f00000.inv()
                require(left.virtualSize == right.virtualSize && left.address == right.address && left.size == right.size && left.flags and runtimeFlags == right.flags and runtimeFlags) { "PE section layout changed: ${left.name}" }
                // BFD also zeroes file-alignment padding beyond VirtualSize; it is not mapped as section data.
                val mappedSize = if (left.virtualSize == 0) left.size else minOf(left.virtualSize, left.size)
                require(original.copyOfRange(left.raw, left.raw + mappedSize).contentEquals(stripped.copyOfRange(right.raw, right.raw + mappedSize))) { "PE runtime data changed: ${left.name}" }
            }
        }
    }
}
