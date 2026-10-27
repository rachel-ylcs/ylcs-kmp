package love.yinlin.repository.nativecompose

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SourceCommandTest {
    private fun probe(directory: File): List<String> {
        val source = directory.resolve("SourceCommandProbe.java")
        source.writeText("""
            public class SourceCommandProbe {
                public static void main(String[] arguments) {
                    System.err.println("Git diagnostic");
                    System.err.print("x".repeat(128 * 1024));
                    System.out.print("https://github.com/JetBrains/compose-multiplatform-core.git");
                    System.out.print((char) 10);
                    if (arguments.length > 0) System.exit(7);
                }
            }
        """.trimIndent())
        val java = File(System.getProperty("java.home"), "bin/${if (File.separatorChar == '\\') "java.exe" else "java"}")
        return listOf(java.path, source.path)
    }

    @Test
    fun successfulCommandsReturnOnlyStdoutWhileDrainingDiagnostics() {
        val directory = Files.createTempDirectory("native-command-streams").toFile()
        try {
            assertEquals("https://github.com/JetBrains/compose-multiplatform-core.git\n", sourceCommand(directory, probe(directory)))
        } finally { assertTrue(directory.deleteRecursively()) }
    }

    @Test
    fun failedCommandsRetainBothOutputStreams() {
        val directory = Files.createTempDirectory("native-command-failure").toFile()
        try {
            val error = assertFailsWith<IllegalStateException> { sourceCommand(directory, probe(directory) + "fail") }
            assertTrue(error.message.orEmpty().contains("failed (7)"))
            assertTrue(error.message.orEmpty().contains("https://github.com/JetBrains/compose-multiplatform-core.git"))
            assertTrue(error.message.orEmpty().contains("Git diagnostic"))
        } finally { assertTrue(directory.deleteRecursively()) }
    }
}
