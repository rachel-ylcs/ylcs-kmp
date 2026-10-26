package love.yinlin.repository.nativecompose

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NativeSourceCacheTest {
    @Test
    fun snapshotExcludesCompilerOutputsAndRestoresGitAndKotlinSources() {
        val work = Files.createTempDirectory("native-source-cache").toFile()
        try {
            val native = work.resolve("native-compose").apply { mkdirs() }
            val vendor = native.resolve("vendor")
            for (root in listOf("skiko/workspace", "compose/workspace", "resources/workspace", "maven/workspace", "maven/states")) vendor.resolve(root).mkdirs()
            vendor.resolve("compose/workspace/src/A.kt").apply {
                parentFile.mkdirs()
                writeText("source")
            }
            vendor.resolve("compose/workspace/.git/HEAD").apply {
                parentFile.mkdirs()
                writeText("locked commit")
            }
            vendor.resolve("compose/workspace/build/classes/Generated.kt").apply {
                parentFile.mkdirs()
                writeText("output")
            }
            vendor.resolve("skiko/workspace/skiko/dependencies/sdk.bin").apply {
                parentFile.mkdirs()
                writeText("SDK")
            }
            val cache = NativeSourceCache(native)
            cache.create()
            val archive = vendor.resolve("cache/prepared-sources.zip")
            ZipFile(archive).use { zip ->
                val names = zip.entries().asSequence().map { it.name }.toSet()
                assertTrue("compose/workspace/src/A.kt" in names && "compose/workspace/.git/HEAD" in names)
                assertFalse(names.any { "/build/" in it || "/dependencies/" in it })
            }
            assertTrue(vendor.resolve("compose/workspace").deleteRecursively())
            cache.restore()
            assertEquals("source", vendor.resolve("compose/workspace/src/A.kt").readText())
            assertEquals("locked commit", vendor.resolve("compose/workspace/.git/HEAD").readText())
        } finally { assertTrue(work.deleteRecursively()) }
    }

    @Test
    fun cacheCannotOverwriteAnExistingSourceOrIntroduceBuildFiles() {
        val work = Files.createTempDirectory("native-source-cache-guard").toFile()
        try {
            val native = work.resolve("native-compose").apply { mkdirs() }
            val existing = native.resolve("vendor/compose/workspace/A.kt").apply {
                parentFile.mkdirs()
                writeText("local")
            }
            val archive = native.resolve("vendor/cache/prepared-sources.zip").apply { parentFile.mkdirs() }
            ZipOutputStream(archive.outputStream()).use {
                it.putNextEntry(ZipEntry("compose/workspace/A.kt"))
                it.write("replacement".toByteArray())
                it.closeEntry()
            }
            assertFailsWith<IllegalStateException> { NativeSourceCache(native).restore() }
            assertEquals("local", existing.readText())
            ZipOutputStream(archive.outputStream()).use {
                it.putNextEntry(ZipEntry("compose/workspace/build/injected.kt"))
                it.write("generated".toByteArray())
                it.closeEntry()
            }
            assertFailsWith<IllegalStateException> { NativeSourceCache(native).restore() }
            assertFalse(native.resolve("vendor/compose/workspace/build/injected.kt").exists())
        } finally { assertTrue(work.deleteRecursively()) }
    }
}
