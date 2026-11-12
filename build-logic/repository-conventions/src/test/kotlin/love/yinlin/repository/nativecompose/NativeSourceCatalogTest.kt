package love.yinlin.repository.nativecompose

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class NativeSourceCatalogTest {
    @Test
    fun onlyVersionsUsedByUpstreamSourcesSelectTheWorkspace() {
        val repository = Files.createTempDirectory("native-generation-versions").toFile()
        try {
            repository.resolve("settings.gradle.kts").writeText("repository { rachel { jvm { version = 25 } } }")
            val file = repository.resolve("libs.versions.toml")
            val original = """
                [versions]
                kotlin = "2.5.0-RC"
                cmp = "1.13.0-beta02"
                skiko = "0.156.1"
                sdl = "3.4.20"
                rachel = "3.7.2"
                coroutines = "1.11.0"
                navigation3 = "1.2.0"
                material3-adaptive = "1.4.0-alpha01"
                icu = "78.2"
                lifecycle = "2.11.0"
                [libraries]
                business = { module = "example:business", version = "1.0.0" }
            """.trimIndent()
            file.writeText(original)
            val modules = mapOf("ui" to emptyMap<String, String>(), "navigation3-ui" to mapOf("versionRef" to "navigation3"), "adaptive" to mapOf("versionRef" to "material3-adaptive"))
            val versions = NativeSourceCatalog(repository).upstreamVersions(modules)
            assertEquals(setOf("kotlin", "cmp", "skiko", "sdl", "icu", "navigation3", "material3-adaptive"), versions.keys)
            for ((name, version) in versions) {
                file.writeText(original.replace("$name = \"$version\"", "$name = \"$version-changed\""))
                assertNotEquals(versions, NativeSourceCatalog(repository).upstreamVersions(modules), name)
            }
            file.writeText(original.replace("3.7.2", "3.7.3").replace("1.11.0", "1.12.0").replace("2.11.0", "2.12.0").replace("example:business", "example:other"))
            repository.resolve("settings.gradle.kts").writeText("repository { rachel { jvm { version = 26 } } }")
            repository.resolve("native-compose/adaptation/fixture/src/main/kotlin/Local.kt").apply {
                parentFile.mkdirs()
                writeText("// changed adaptation source")
            }
            assertEquals(versions, NativeSourceCatalog(repository).upstreamVersions(modules))
            val extended = modules + ("new-upstream" to mapOf("versionRef" to "lifecycle"))
            assertEquals("2.12.0", NativeSourceCatalog(repository).upstreamVersions(extended)["lifecycle"])
            file.writeText(original.replace("2.5.0-RC", "../invalid"))
            assertFailsWith<IllegalStateException> { NativeSourceCatalog(repository).upstreamVersions(modules) }
        } finally { assertTrue(repository.deleteRecursively()) }
    }
}
