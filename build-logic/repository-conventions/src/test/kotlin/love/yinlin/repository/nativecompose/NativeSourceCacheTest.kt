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
    fun snapshotRestoresDetachedGitRepositoriesWithEmptyRefDirectories() {
        val work = Files.createTempDirectory("native-source-cache-detached").toFile()
        try {
            sourceGit(work, "init", "-q")
            sourceGit(work, "remote", "add", "origin", "https://example.invalid/outer.git")
            val native = work.resolve("native-compose").apply { mkdirs() }
            val vendor = native.resolve("vendor")
            for (root in listOf("skiko/workspace", "compose/workspace", "resources/workspace", "maven/workspace", "maven/states", "icu/workspace")) vendor.resolve(root).mkdirs()
            val commits = linkedMapOf<String, String>()
            for (repository in listOf("compose", "resources")) {
                val workspace = vendor.resolve("$repository/workspace")
                workspace.resolve("A.kt").writeText("source\n")
                sourceGit(workspace, "-c", "init.defaultBranch=fixture", "init", "-q")
                sourceGit(workspace, "remote", "add", "origin", "https://example.invalid/$repository.git")
                sourceGit(workspace, "add", ".")
                sourceGit(workspace, "-c", "user.name=Native Source Fixture", "-c", "user.email=fixture@example.invalid", "-c", "commit.gpgsign=false", "commit", "-qm", "fixture")
                commits[repository] = sourceGit(workspace, "rev-parse", "HEAD").trim()
                sourceGit(workspace, "checkout", "--detach")
                sourceGit(workspace, "branch", "-D", "fixture")
                assertFalse(workspace.resolve(".git/refs").walkTopDown().any { it.isFile })
            }
            val cache = NativeSourceCache(native)
            cache.create()
            for (repository in commits.keys) assertTrue(vendor.resolve("$repository/workspace").renameTo(work.resolve("original-$repository")))
            cache.restore()
            for ((repository, commit) in commits) {
                val workspace = vendor.resolve("$repository/workspace")
                // Match CI, where actions/checkout also trusts the enclosing repository.
                assertEquals("https://example.invalid/$repository.git", sourceGit(workspace, "-c", "safe.directory=${work.canonicalFile.invariantSeparatorsPath}", "config", "--local", "--get-all", "remote.origin.url").trim())
                assertEquals(commit, sourceGit(workspace, "rev-parse", "HEAD").trim())
                assertTrue(workspace.resolve(".git/refs").isDirectory)
                assertTrue(workspace.resolve(".git/objects").isDirectory)
                assertEquals("source\n", workspace.resolve("A.kt").readText())
            }
        } finally {
            for (file in work.walkTopDown().filter { it.isFile }) assertTrue(file.setWritable(true))
            assertTrue(work.deleteRecursively())
        }
    }

    @Test
    fun snapshotExcludesCompilerOutputsAndRestoresGitAndKotlinSources() {
        val work = Files.createTempDirectory("native-source-cache").toFile()
        try {
            val native = work.resolve("native-compose").apply { mkdirs() }
            native.resolve("upstream/sdl.json").apply {
                parentFile.mkdirs()
                writeText("""{"version":"3.4.18"}""")
            }
            val vendor = native.resolve("vendor")
            vendor.resolve("sdl-source-3.4.18/include/SDL3/SDL.h").apply {
                parentFile.mkdirs()
                writeText("SDL source header")
            }
            for (root in listOf("skiko/workspace", "compose/workspace", "resources/workspace", "maven/workspace", "maven/states", "icu/workspace")) vendor.resolve(root).mkdirs()
            vendor.resolve("icu/workspace/icudtl.dat").writeText("locale data")
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
                assertTrue("icu/workspace/icudtl.dat" in names)
                assertTrue("sdl-source-3.4.18/include/SDL3/SDL.h" in names)
                assertFalse(names.any { "/build/" in it || "/dependencies/" in it })
            }
            assertTrue(vendor.resolve("compose/workspace").deleteRecursively())
            assertTrue(vendor.resolve("icu").deleteRecursively())
            assertTrue(vendor.resolve("sdl-source-3.4.18").deleteRecursively())
            cache.restore()
            assertEquals("source", vendor.resolve("compose/workspace/src/A.kt").readText())
            assertEquals("locked commit", vendor.resolve("compose/workspace/.git/HEAD").readText())
            assertEquals("locale data", vendor.resolve("icu/workspace/icudtl.dat").readText())
            assertEquals("SDL source header", vendor.resolve("sdl-source-3.4.18/include/SDL3/SDL.h").readText())
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
