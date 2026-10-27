package love.yinlin.repository.nativecompose

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SourceWorkspaceTest {
    @Test
    fun reuseValidatesSourcesWithoutWritingAndMissingFilesCanBeRepaired() {
        val vendor = Files.createTempDirectory("native-source-reuse").toFile()
        try {
            val staging = vendor.resolve("staging").apply { mkdirs() }
            staging.resolve("A.kt").writeText("prepared")
            val destination = vendor.resolve("workspace")
            val record = vendor.resolve("state.json")
            val identity = mapOf("fingerprint" to "locked inputs")
            val workspace = SourceWorkspace(vendor)
            workspace.synchronize(staging, destination, record, identity)
            val file = destination.resolve("A.kt")
            assertTrue(file.setLastModified(1234567890000L))
            val timestamp = file.lastModified()
            assertTrue(workspace.reusable(destination, record, identity))
            assertEquals(timestamp, file.lastModified())
            assertFalse(workspace.reusable(destination, record, mapOf("fingerprint" to "updated inputs")))
            assertTrue(file.delete())
            assertFalse(workspace.reusable(destination, record, identity))
            workspace.synchronize(staging, destination, record, identity)
            assertEquals("prepared", file.readText())
        } finally { assertTrue(vendor.deleteRecursively()) }
    }

    @Test
    fun reuseRejectsModifiedAndUnrecordedSources() {
        val vendor = Files.createTempDirectory("native-source-reuse-guard").toFile()
        try {
            val staging = vendor.resolve("staging").apply { mkdirs() }
            staging.resolve("src/A.kt").apply {
                parentFile.mkdirs()
                writeText("prepared")
            }
            val destination = vendor.resolve("workspace")
            val record = vendor.resolve("state.json")
            val identity = mapOf("fingerprint" to "locked inputs")
            val workspace = SourceWorkspace(vendor)
            workspace.synchronize(staging, destination, record, identity)
            val file = destination.resolve("src/A.kt")
            file.writeText("local edit")
            assertFailsWith<IllegalStateException> { workspace.reusable(destination, record, identity) }
            assertEquals("local edit", file.readText())
            file.writeText("prepared")
            destination.resolve("src/Extra.kt").writeText("local source")
            assertFailsWith<IllegalStateException> { workspace.reusable(destination, record, identity) }
            assertEquals("local source", destination.resolve("src/Extra.kt").readText())
        } finally { assertTrue(vendor.deleteRecursively()) }
    }

    @Test
    fun restoredSdkDoesNotPreventFirstSourcePreparation() {
        val vendor = Files.createTempDirectory("native-source-sdk-cache").toFile()
        try {
            val staging = vendor.resolve("staging").apply { mkdirs() }
            staging.resolve("settings.gradle.kts").writeText("upstream settings")
            val destination = vendor.resolve("workspace")
            val sdk = destination.resolve("skiko/dependencies/skia/locked-version/sdk.bin").apply {
                parentFile.mkdirs()
                writeText("cached SDK")
            }
            val record = destination.resolve(".rachel-prepared.json")
            val hashes = SourceWorkspace(vendor).synchronize(staging, destination, record, mapOf("schema" to 3), setOf("skiko/dependencies/skia"))
            assertEquals("upstream settings", destination.resolve("settings.gradle.kts").readText())
            assertEquals("cached SDK", sdk.readText())
            assertEquals(setOf("settings.gradle.kts"), hashes.keys)
            assertTrue(record.isFile)
        } finally { assertTrue(vendor.deleteRecursively()) }
    }

    @Test
    fun restoredSdkDoesNotAllowUnknownWorkspaceFilesToBeOverwritten() {
        val vendor = Files.createTempDirectory("native-source-sdk-guard").toFile()
        try {
            val staging = vendor.resolve("staging").apply { mkdirs() }
            staging.resolve("settings.gradle.kts").writeText("upstream settings")
            val destination = vendor.resolve("workspace")
            val sdk = destination.resolve("skiko/dependencies/skia/sdk.bin").apply {
                parentFile.mkdirs()
                writeText("cached SDK")
            }
            val local = destination.resolve("settings.gradle.kts").apply { writeText("local settings") }
            val record = destination.resolve(".rachel-prepared.json")
            assertFailsWith<IllegalStateException> { SourceWorkspace(vendor).synchronize(staging, destination, record, mapOf("schema" to 3), setOf("skiko/dependencies/skia")) }
            assertEquals("local settings", local.readText())
            assertEquals("cached SDK", sdk.readText())
            assertFalse(record.exists())
        } finally { assertTrue(vendor.deleteRecursively()) }
    }

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
