package love.yinlin.nativebinary

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class NativeReleaseSymbolsTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test
    fun toolFailureLeavesTheLinkedBinaryIntactAndRemovesTemporaryFiles() {
        val original = byteArrayOf(1, 2, 3, 4)
        val binary = temporary.newFile("app.kexe").apply { writeBytes(original) }
        val symbols = temporary.root.resolve("symbols")
        assertFailsWith<IOException> {
            NativeReleaseSymbols.separate(binary, symbols, "linux_x64", temporary.root, temporary.root.resolve("missing-objcopy").path)
        }
        assertContentEquals(original, binary.readBytes())
        assertFalse(symbols.resolve("symbols.properties").exists())
        assertFalse(temporary.root.listFiles()!!.any { it.name.startsWith(".release-symbols-") })
    }
}
