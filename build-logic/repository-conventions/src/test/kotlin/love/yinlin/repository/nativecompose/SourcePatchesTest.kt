package love.yinlin.repository.nativecompose

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SourcePatchesTest {
    @Test
    fun windowsCheckoutPreservesTheBytesUsedByTheLockedPatchHash() {
        val work = Files.createTempDirectory("native-patch-checkout").toFile()
        try {
            val repository = File(checkNotNull(System.getProperty("native.sources.repositoryRoot")))
            repository.resolve(".gitattributes").copyTo(work.resolve(".gitattributes"))
            val native = work.resolve("native-compose")
            val patch = native.resolve("upstream/patches/fixture.patch").apply { parentFile.mkdirs() }
            val bytes = "diff --git a/Fixture.kt b/Fixture.kt\n--- a/Fixture.kt\n+++ b/Fixture.kt\n@@ -1 +1 @@\n-old\n+new\n".toByteArray()
            patch.writeBytes(bytes)
            val specification = mapOf("file" to patch.name, "sha256" to bytesHash(bytes))
            native.resolve("upstream/patches/manifest.json").writeBytes(sourceBytes(mapOf("schema" to 1, "fixture" to specification)))
            sourceGit(work, "init", "-q")
            val relative = patch.relativeTo(work).invariantSeparatorsPath
            sourceGit(work, "-c", "core.autocrlf=true", "add", "--", ".gitattributes", relative)
            assertTrue(patch.delete())
            sourceGit(work, "-c", "core.autocrlf=true", "checkout-index", "--force", "--", relative)
            assertContentEquals(bytes, patch.readBytes())
            assertEquals(patch, SourcePatches(native).file(specification))
            patch.appendText("changed\n")
            assertFailsWith<IllegalStateException> { SourcePatches(native).file(specification) }
        } finally {
            // Git makes loose object files read-only on Windows; this fixture owns them.
            for (file in work.resolve(".git/objects").walkTopDown().filter { it.isFile }) assertTrue(file.setWritable(true))
            assertTrue(work.deleteRecursively())
        }
    }
}
