package love.yinlin.repository.nativecompose

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SourceArchiveTest {
    @Test
    fun traversalIsRejectedBeforeAnyFilesAreWritten() {
        val work = Files.createTempDirectory("native-source-archive").toFile()
        try {
            val archive = work.resolve("source.zip")
            ZipOutputStream(archive.outputStream()).use {
                it.putNextEntry(ZipEntry("safe.kt"))
                it.write("safe".toByteArray())
                it.closeEntry()
                it.putNextEntry(ZipEntry("../outside.kt"))
                it.write("escape".toByteArray())
                it.closeEntry()
            }
            val output = work.resolve("output")
            assertFailsWith<IllegalStateException> { SourceArchive.extract(archive, output, false) }
            assertFalse(output.resolve("safe.kt").exists())
            assertFalse(work.resolve("outside.kt").exists())
        } finally { assertTrue(work.deleteRecursively()) }
    }

    @Test
    fun unixSymlinksAreRejectedEvenOnWindows() {
        val work = Files.createTempDirectory("native-source-link").toFile()
        try {
            val archive = work.resolve("source.zip")
            ZipOutputStream(archive.outputStream()).use {
                it.putNextEntry(ZipEntry("link"))
                it.write("../outside".toByteArray())
                it.closeEntry()
            }
            val bytes = archive.readBytes()
            val central = (0 .. bytes.size - 46).first { ByteBuffer.wrap(bytes, it, 4).order(ByteOrder.LITTLE_ENDIAN).int == 0x02014b50 }
            ByteBuffer.wrap(bytes, central + 38, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(0xa1ff shl 16)
            archive.writeBytes(bytes)
            assertFailsWith<IllegalStateException> { SourceArchive.extract(archive, work.resolve("output"), false) }
            assertFalse(work.resolve("output/link").exists())
        } finally { assertTrue(work.deleteRecursively()) }
    }

    @Test
    fun rootStrippingAndSourceSelectionKeepOnlyTheRequestedInputs() {
        val work = Files.createTempDirectory("native-source-select").toFile()
        try {
            val archive = work.resolve("source.zip")
            ZipOutputStream(archive.outputStream()).use {
                for (name in listOf("upstream/src/commonMain/A.kt", "upstream/LICENSE", "upstream/unrelated.bin")) {
                    it.putNextEntry(ZipEntry(name))
                    it.write(name.toByteArray())
                    it.closeEntry()
                }
            }
            val output = work.resolve("output")
            val hashes = SourceArchive.extract(archive, output, true) { if (it.endsWith(".kt") || it == "LICENSE") it else null }
            assertEquals(setOf("src/commonMain/A.kt", "LICENSE"), hashes.keys)
            assertEquals("upstream/src/commonMain/A.kt", output.resolve("src/commonMain/A.kt").readText())
            assertFalse(output.resolve("unrelated.bin").exists())
        } finally { assertTrue(work.deleteRecursively()) }
    }
}
