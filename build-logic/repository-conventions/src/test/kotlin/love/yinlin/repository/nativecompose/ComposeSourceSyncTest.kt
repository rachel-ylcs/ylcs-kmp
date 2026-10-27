package love.yinlin.repository.nativecompose

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ComposeSourceSyncTest {
    @Test
    fun preparedGitSourcesAreReusedOfflineWithoutReapplyingPatchesOrChangingTimestamps() {
        val repository = Files.createTempDirectory("native-compose-git-reuse").toFile()
        try {
            val original = File(checkNotNull(System.getProperty("native.sources.repositoryRoot")))
            original.resolve("libs.versions.toml").copyTo(repository.resolve("libs.versions.toml"))
            repository.resolve("settings.gradle.kts").writeText("repository { rachel { jvm { version = 25 } } }")
            val native = repository.resolve("native-compose")
            assertTrue(original.resolve("native-compose/upstream").copyRecursively(native.resolve("upstream")))
            val sync = ComposeSourceSync(NativeUpstreamSync(native))
            val owner = native.resolve("vendor/compose").apply { mkdirs() }
            val workspace = owner.resolve("workspace").apply { mkdirs() }
            val source = workspace.resolve("src/A.kt").apply {
                parentFile.mkdirs()
                writeText("old\n")
            }
            val other = workspace.resolve("src/Other.kt").apply { writeText("unchanged\n") }
            sourceGit(workspace, "init", "-q")
            sourceGit(workspace, "remote", "add", "origin", "https://github.com/JetBrains/compose-multiplatform-core.git")
            sourceGit(workspace, "add", ".")
            sourceGit(workspace, "-c", "user.name=Native Source Fixture", "-c", "user.email=fixture@example.invalid", "-c", "commit.gpgsign=false", "commit", "-qm", "fixture")
            val commit = sourceGit(workspace, "rev-parse", "HEAD").trim()
            val patch = native.resolve("upstream/patches/fixture.patch").apply {
                writeText("diff --git a/src/A.kt b/src/A.kt\n--- a/src/A.kt\n+++ b/src/A.kt\n@@ -1 +1 @@\n-old\n+prepared\n")
            }
            val specification = mapOf("file" to patch.name, "sha256" to patch.sourceHash())
            val identity = mapOf("repository" to "JetBrains/compose-multiplatform-core", "commit" to commit)
            val marker = owner.resolve("checkout.txt").apply { writeText("$commit\nsrc\n") }
            val record = owner.resolve("source-edits.json")
            sync.prepareGit(workspace, record, marker, identity, listOf("src"), specification, true)
            assertEquals("prepared\n", source.readText())
            assertTrue(source.setLastModified(1234567890000L))
            val timestamp = source.lastModified()
            sync.prepareGit(workspace, record, marker, identity, listOf("src"), specification, true)
            assertEquals(timestamp, source.lastModified())
            assertEquals("prepared\n", source.readText())
            other.writeText("local edit\n")
            assertFailsWith<IllegalStateException> { sync.prepareGit(workspace, record, marker, identity, listOf("src"), specification, true) }
            assertEquals("local edit\n", other.readText())
            assertEquals(timestamp, source.lastModified())
            other.writeText("unchanged\n")
            workspace.resolve("src/Extra.kt").writeText("local source\n")
            assertFailsWith<IllegalStateException> { sync.prepareGit(workspace, record, marker, identity, listOf("src"), specification, true) }
            assertEquals("local source\n", workspace.resolve("src/Extra.kt").readText())
        } finally {
            for (file in repository.walkTopDown().filter { it.isFile }) assertTrue(file.setWritable(true))
            assertTrue(repository.deleteRecursively())
        }
    }
}
