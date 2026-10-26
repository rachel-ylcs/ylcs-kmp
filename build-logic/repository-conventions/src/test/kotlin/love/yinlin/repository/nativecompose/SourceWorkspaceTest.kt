package love.yinlin.repository.nativecompose

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SourceWorkspaceTest {
    @Test
    fun localEditsArePreservedBeforeAnotherFileCanBeChanged() {
        val vendor = Files.createTempDirectory("native-source-workspace").toFile()
        try {
            val staging = vendor.resolve("staging").apply { mkdirs() }
            staging.resolve("A.kt").writeText("original")
            val destination = vendor.resolve("workspace")
            val record = vendor.resolve("state.json")
            val workspace = SourceWorkspace(vendor)
            workspace.synchronize(staging, destination, record, mapOf("schema" to 3))
            destination.resolve("A.kt").writeText("local change")
            staging.resolve("A.kt").writeText("new upstream")
            staging.resolve("B.kt").writeText("new file")
            assertFailsWith<IllegalStateException> { workspace.synchronize(staging, destination, record, mapOf("schema" to 3)) }
            assertEquals("local change", destination.resolve("A.kt").readText())
            assertFalse(destination.resolve("B.kt").exists())
        } finally { assertTrue(vendor.deleteRecursively()) }
    }

    @Test
    fun synchronizationRetainsBuildOutputsAndUnchangedFileTimestamps() {
        val vendor = Files.createTempDirectory("native-source-output").toFile()
        try {
            val staging = vendor.resolve("staging").apply { mkdirs() }
            staging.resolve("A.kt").writeText("unchanged")
            val destination = vendor.resolve("workspace")
            val record = vendor.resolve("state.json")
            val workspace = SourceWorkspace(vendor)
            workspace.synchronize(staging, destination, record, mapOf("schema" to 3))
            val file = destination.resolve("A.kt")
            assertTrue(file.setLastModified(1234567890000L))
            val timestamp = file.lastModified()
            destination.resolve("build/output.bin").apply {
                parentFile.mkdirs()
                writeText("built")
            }
            workspace.synchronize(staging, destination, record, mapOf("schema" to 3))
            assertEquals(timestamp, file.lastModified())
            assertEquals("built", destination.resolve("build/output.bin").readText())
        } finally { assertTrue(vendor.deleteRecursively()) }
    }
}
