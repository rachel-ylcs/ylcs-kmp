package love.yinlin.nativebridge

import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Updates only the staged PE image, using Windows' resource APIs through JDK 25. */
internal object NativeWindowsResources {
    fun write(executable: File, desktop: NativeDesktopDistribution, icon: File?) {
        require(System.getProperty("os.name").startsWith("Windows"))
        Arena.ofConfined().use { arena ->
            val linker = Linker.nativeLinker()
            val kernel = SymbolLookup.libraryLookup("Kernel32.dll", arena)
            val begin = linker.downcallHandle(kernel.find("BeginUpdateResourceW").orElseThrow(), FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT))
            val update = linker.downcallHandle(kernel.find("UpdateResourceW").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_SHORT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT))
            val end = linker.downcallHandle(kernel.find("EndUpdateResourceW").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT))
            val fileName = executable.absolutePath.toByteArray(Charsets.UTF_16LE) + byteArrayOf(0, 0)
            val name = arena.allocate(fileName.size.toLong()).also { it.copyFrom(MemorySegment.ofArray(fileName)) }
            val handle = begin.invokeWithArguments(name, 0) as MemorySegment
            check(handle.address() != 0L) { "Cannot open the staged executable's Windows resources: $executable" }
            var active = true
            try {
                val entries = mutableListOf(Triple(16, 1, versionInfo(desktop, executable.name)))
                if (icon != null) entries += iconResources(icon.readBytes())
                for ((type, id, bytes) in entries) {
                    val data = arena.allocate(bytes.size.toLong()).also { it.copyFrom(MemorySegment.ofArray(bytes)) }
                    check(update.invokeWithArguments(handle, MemorySegment.ofAddress(type.toLong()), MemorySegment.ofAddress(id.toLong()), 0.toShort(), data, bytes.size) as Int != 0) { "Cannot write Windows resource $type/$id to $executable" }
                    if (type == 14) {
                        val iconName = "SDL_ICON\u0000".toByteArray(Charsets.UTF_16LE)
                        val iconKey = arena.allocate(iconName.size.toLong()).also { it.copyFrom(MemorySegment.ofArray(iconName)) }
                        check(update.invokeWithArguments(handle, MemorySegment.ofAddress(14), iconKey, 0.toShort(), data, bytes.size) as Int != 0) { "Cannot write the default SDL window icon" }
                    }
                }
                val committed = end.invokeWithArguments(handle, 0) as Int
                active = false
                check(committed != 0) { "Cannot commit Windows application resources: $executable" }
            }
            finally {
                if (active) end.invokeWithArguments(handle, 1)
            }
        }
    }

    internal fun iconResources(bytes: ByteArray): List<Triple<Int, Int, ByteArray>> {
        require(bytes.size in 22 .. 16 * 1024 * 1024) { "ICO size must be between 22 bytes and 16 MiB" }
        val input = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(input.short.toInt() == 0 && input.short.toInt() == 1) { "Invalid ICO header" }
        val count = input.short.toInt() and 0xffff
        require(count in 1 .. 256 && 6 + count * 16 <= bytes.size) { "Invalid ICO directory" }
        val group = ByteBuffer.allocate(6 + count * 14).order(ByteOrder.LITTLE_ENDIAN)
        group.putShort(0).putShort(1).putShort(count.toShort())
        val resources = mutableListOf<Triple<Int, Int, ByteArray>>()
        for (index in 0 ..< count) {
            val entry = 6 + index * 16
            val size = input.getInt(entry + 8)
            val offset = input.getInt(entry + 12)
            require(size > 0 && offset >= 6 + count * 16 && offset.toLong() + size <= bytes.size) { "ICO image range is invalid" }
            group.put(bytes, entry, 12).putShort((index + 1).toShort())
            resources += Triple(3, index + 1, bytes.copyOfRange(offset, offset + size))
        }
        resources += Triple(14, 1, group.array())
        return resources
    }

    internal fun versionInfo(desktop: NativeDesktopDistribution, filename: String): ByteArray {
        val numbers = Regex("\\d+").findAll(desktop.packageVersion).take(4).map { it.value.toInt() }.toList()
        require(numbers.isNotEmpty() && numbers.all { it in 0 .. 65535 }) { "Windows packageVersion needs numeric components in 0..65535" }
        val parts = List(4) { numbers.getOrElse(it) { 0 } }
        val high = (parts[0] shl 16) or parts[1]
        val low = (parts[2] shl 16) or parts[3]
        val fixed = ByteBuffer.allocate(52).order(ByteOrder.LITTLE_ENDIAN)
        for (number in listOf(0xFEEF04BD.toInt(), 0x10000, high, low, high, low, 0x3f, 0, 0x40004, 1, 0, 0, 0)) fixed.putInt(number)
        val values = linkedMapOf("CompanyName" to desktop.vendor, "FileDescription" to desktop.displayName, "FileVersion" to desktop.packageVersion, "InternalName" to desktop.packageName, "LegalCopyright" to desktop.copyright, "OriginalFilename" to filename, "ProductName" to desktop.displayName, "ProductVersion" to desktop.packageVersion, "Comments" to desktop.description, "Author" to desktop.author)
        val strings = values.map { (key, value) -> versionNode(key, (value + '\u0000').toByteArray(Charsets.UTF_16LE), true) }
        val stringInfo = versionNode("StringFileInfo", byteArrayOf(), true, listOf(versionNode("040904B0", byteArrayOf(), true, strings)))
        val translation = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putShort(0x409).putShort(1200).array()
        val variableInfo = versionNode("VarFileInfo", byteArrayOf(), true, listOf(versionNode("Translation", translation, false)))
        return versionNode("VS_VERSION_INFO", fixed.array(), false, listOf(stringInfo, variableInfo))
    }

    private fun versionNode(key: String, value: ByteArray, text: Boolean, children: List<ByteArray> = emptyList()): ByteArray {
        val output = ByteArrayOutputStream()
        output.write(ByteArray(6))
        output.write((key + '\u0000').toByteArray(Charsets.UTF_16LE))
        while (output.size() % 4 != 0) output.write(0)
        output.write(value)
        for (child in children) {
            while (output.size() % 4 != 0) output.write(0)
            output.write(child)
        }
        val bytes = output.toByteArray()
        require(bytes.size <= 65535) { "Windows version block exceeds 65535 bytes" }
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putShort(bytes.size.toShort()).putShort((if (text) value.size / 2 else value.size).toShort()).putShort((if (text) 1 else 0).toShort())
        return bytes
    }
}
