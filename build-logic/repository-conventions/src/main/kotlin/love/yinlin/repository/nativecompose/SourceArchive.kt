package love.yinlin.repository.nativecompose

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Bounded ZIP handling, including UNIX symlink rejection on Windows hosts. */
internal object SourceArchive {
    private fun short(bytes: ByteArray, offset: Int): Int = ByteBuffer.wrap(bytes, offset, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xffff
    private fun integer(bytes: ByteArray, offset: Int): Long = ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xffffffffL

    private fun rejectLinks(file: File) {
        RandomAccessFile(file, "r").use { input ->
            check(input.length() >= 22) { "Truncated ZIP archive: $file" }
            val tail = ByteArray(minOf(input.length(), 65557).toInt())
            val tailStart = input.length() - tail.size
            input.seek(tailStart)
            input.readFully(tail)
            val end = (tail.size - 22 downTo 0).firstOrNull { integer(tail, it) == 0x06054b50L && it + 22 + short(tail, it + 20) == tail.size } ?: error("Invalid ZIP directory: $file")
            var count = short(tail, end + 10).toLong()
            var size = integer(tail, end + 12)
            var offset = integer(tail, end + 16)
            check(short(tail, end + 4) == 0 && short(tail, end + 6) == 0) { "Multipart source ZIP is unsupported" }
            if (count == 65535L || size == 0xffffffffL || offset == 0xffffffffL) {
                input.seek(tailStart + end - 20)
                val locator = ByteArray(20).also { input.readFully(it) }
                check(integer(locator, 0) == 0x07064b50L && integer(locator, 4) == 0L && integer(locator, 16) == 1L) { "Invalid ZIP64 locator" }
                val recordOffset = ByteBuffer.wrap(locator, 8, 8).order(ByteOrder.LITTLE_ENDIAN).long
                check(recordOffset >= 0 && recordOffset <= input.length() - 56)
                input.seek(recordOffset)
                val record = ByteArray(56).also { input.readFully(it) }
                check(integer(record, 0) == 0x06064b50L && integer(record, 16) == 0L && integer(record, 20) == 0L)
                count = ByteBuffer.wrap(record, 32, 8).order(ByteOrder.LITTLE_ENDIAN).long
                size = ByteBuffer.wrap(record, 40, 8).order(ByteOrder.LITTLE_ENDIAN).long
                offset = ByteBuffer.wrap(record, 48, 8).order(ByteOrder.LITTLE_ENDIAN).long
            }
            check(count in 0 .. 100_000 && size >= 0 && offset >= 0 && offset <= input.length() - size) { "Oversized or invalid ZIP directory" }
            input.seek(offset)
            repeat(count.toInt()) {
                val entry = ByteArray(46).also { input.readFully(it) }
                check(integer(entry, 0) == 0x02014b50L) { "Invalid ZIP directory entry" }
                check((integer(entry, 38).ushr(16).toInt() and 0xf000) != 0xa000) { "Symlink in source archive: $file" }
                input.seek(input.filePointer + short(entry, 28) + short(entry, 30) + short(entry, 32))
                check(input.filePointer <= offset + size) { "ZIP directory entry escapes its bounds" }
            }
        }
    }

    fun extract(file: File, destination: File, stripRoot: Boolean, select: (String) -> String? = { it }): Map<String, String> {
        rejectLinks(file)
        destination.mkdirs()
        val plan = linkedMapOf<String, ZipEntry>()
        var total = 0L
        ZipFile(file).use { archive ->
            for (entry in archive.entries().asSequence()) {
                val name = entry.name.removeSuffix("/")
                if (name.isEmpty()) continue
                sourceRelativePath(name)
                val relative = if (stripRoot) name.substringAfter('/', "") else name
                if (relative.isEmpty() || entry.isDirectory) continue
                val selected = select(relative) ?: continue
                sourceRelativePath(selected)
                checkedSourcePath(destination, destination.resolve(selected))
                total += entry.size
                check(entry.size in 0 .. 256L * 1024 * 1024 && total <= 4L * 1024 * 1024 * 1024 && plan.size < 100_000 && plan.put(selected, entry) == null) { "Source ZIP size/count/duplicate limit: $file" }
            }
            val hashes = linkedMapOf<String, String>()
            for ((relative, entry) in plan) {
                val target = destination.resolve(relative)
                target.parentFile.mkdirs()
                archive.getInputStream(entry).use { input ->
                    target.outputStream().use { output ->
                        var count = 0L
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            count += read
                            check(count <= entry.size) { "ZIP entry exceeds its declared size: ${entry.name}" }
                            output.write(buffer, 0, read)
                        }
                        check(count == entry.size) { "ZIP entry size changed: ${entry.name}" }
                    }
                }
                if (entry.comment?.startsWith("rachel-mode:") == true && File.separatorChar != '\\') {
                    val mode = entry.comment.removePrefix("rachel-mode:").toInt()
                    check(mode in 0 .. 511)
                    val permissions = PosixFilePermission.entries.filterIndexed { index, _ -> mode and (1 shl (8 - index)) != 0 }.toSet()
                    Files.setPosixFilePermissions(target.toPath(), permissions)
                }
                hashes[relative] = target.sourceHash()
            }
            return hashes
        }
    }

    fun snapshot(files: Map<String, File>, destination: File) {
        destination.parentFile.mkdirs()
        val temporary = destination.resolveSibling("${destination.name}.tmp")
        try {
            ZipOutputStream(temporary.outputStream().buffered()).use { archive ->
                var total = 0L
                check(files.size <= 100_000)
                for ((name, file) in files.toSortedMap()) {
                    sourceRelativePath(name)
                    check(file.isFile && !Files.isSymbolicLink(file.toPath())) { "Invalid source cache input: $file" }
                    total += file.length()
                    check(file.length() <= 256L * 1024 * 1024 && total <= 4L * 1024 * 1024 * 1024) { "Source cache exceeds its size limit" }
                    val entry = ZipEntry(name)
                    if (File.separatorChar != '\\') {
                        val permissions = Files.getPosixFilePermissions(file.toPath())
                        entry.comment = "rachel-mode:" + PosixFilePermission.entries.foldIndexed(0) { index, mode, permission -> if (permission in permissions) mode or (1 shl (8 - index)) else mode }
                    }
                    archive.putNextEntry(entry)
                    file.inputStream().use { it.transferTo(archive) }
                    archive.closeEntry()
                }
            }
            Files.move(temporary.toPath(), destination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        } finally { temporary.delete() }
    }
}
