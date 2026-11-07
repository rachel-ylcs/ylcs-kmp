package love.yinlin.repository.nativecompose

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class NativeSourceInputsTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test
    fun offlineArchiveLookupSeparatesVersionsAndRejectsCorruptedBytes() {
        val repository = temporary.root
        repository.resolve("libs.versions.toml").writeText("[versions]\nsdl = \"3.4.20\"\n[libraries]\n")
        repository.resolve("settings.gradle.kts").writeText("repository { rachel { jvm { version = 25 } } }")
        val nativeDirectory = repository.resolve("native-compose")
        nativeDirectory.resolve("upstream/patches/manifest.json").apply {
            parentFile.mkdirs()
            writeText("{}")
        }
        val sync = NativeUpstreamSync(nativeDirectory)
        val sources = NativeSourceInputs(sync, offline = true)
        val oldOutput = nativeDirectory.resolve("vendor/sdl-static/mingw_x64/lib/SDL3-static.lib").apply {
            parentFile.mkdirs()
            writeText("legacy SDK")
        }
        val bytes = "source archive fixture".toByteArray()
        val sha = bytesHash(bytes)
        fun url(version: String) = "https://github.com/libsdl-org/SDL/releases/download/release-$version/SDL3-$version.zip"
        fun cached(version: String) = nativeDirectory.resolve("vendor/archives/sdl/$version/$sha.zip").apply {
            parentFile.mkdirs()
            writeBytes(bytes)
            val location = url(version)
            resolveSibling("${bytesHash(location.toByteArray())}.json").writeBytes(sourceBytes(mapOf("version" to version, "url" to location, "sha256" to sha)))
        }
        val old = cached("3.4.18")
        assertEquals(old, sources.archive("sdl", "3.4.18", url("3.4.18"), 1024).file)
        val missing = assertFailsWith<IllegalStateException> { sources.archive("sdl", "3.4.20", url("3.4.20"), 1024) }
        assertTrue(missing.message.orEmpty().contains("Offline upstream archive is missing"))
        val selected = cached("3.4.20")
        assertNotEquals(old, sources.archive("sdl", "3.4.20", url("3.4.20"), 1024).file)
        selected.writeText("corrupted")
        val corrupt = assertFailsWith<IllegalStateException> { sources.archive("sdl", "3.4.20", url("3.4.20"), 1024) }
        assertTrue(corrupt.message.orEmpty().contains("SHA-256 mismatch"))
        assertEquals("legacy SDK", oldOutput.readText())
        assertTrue(old.isFile)
        assertFalse(nativeDirectory.resolve("vendor/current.json").exists())
    }

    @Test
    fun preparedWorkspacePreservesLocalFilesAndReusesUnchangedSources() {
        val vendor = temporary.newFolder("vendor")
        val generation = vendor.resolve("g/current")
        val workspace = SourceWorkspace(vendor)
        val destination = generation.resolve("compose/workspace")
        val record = generation.resolve("compose/files.json")
        val identity = mapOf("sha256" to bytesHash("upstream".toByteArray()))
        val source = "ui/src/commonMain/kotlin/A.kt"
        val staging = temporary.newFolder("staging")
        staging.resolve(source).apply {
            parentFile.mkdirs()
            writeText("// prepared source\n")
        }
        workspace.synchronize(staging, destination, record, identity)
        val prepared = destination.resolve(source)
        assertTrue(prepared.setLastModified(1234567890000L))
        val timestamp = prepared.lastModified()
        assertTrue(workspace.reusable(destination, record, identity))
        assertEquals(timestamp, prepared.lastModified())

        val extra = prepared.resolveSibling("Extra.kt").apply { writeText("// manually added\n") }
        assertFailsWith<IllegalStateException> { workspace.reusable(destination, record, identity) }
        assertEquals("// manually added\n", extra.readText())
        assertTrue(extra.delete())

        prepared.writeText("// local edit\n")
        assertFailsWith<IllegalStateException> { workspace.synchronize(staging, destination, record, identity) }
        assertEquals("// local edit\n", prepared.readText())
    }
}
