package love.yinlin.repository.nativecompose

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class NativeCiInputsTest {
    @Test
    fun applicationVersionsDoNotInvalidateNativeInputsButDependenciesAndPluginsDo() {
        val repository = Files.createTempDirectory("native-ci-inputs").toFile()
        try {
            repository.resolve("settings.gradle.kts").writeText("repository { rachel { jvm { version = 25 } } }")
            repository.resolve("native-compose/adaptation/fixture/build.gradle.kts").apply {
                parentFile.mkdirs()
                writeText("implementation(libs.androidx.lifecycle)\nplugins {\n    alias(libs.plugins.composeCompiler)\n    alias(libs.plugins.nativeBridge)\n}")
            }
            val catalog = repository.resolve("libs.versions.toml")
            val original = """
                [versions]
                kotlin = "2.5.0-Beta2"
                rachel = "3.7.1"
                business = "1.0.0"
                lifecycle = "2.11.0"
                [libraries]
                androidx-lifecycle = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
                business-client = { module = "example:client", version.ref = "business" }
                [plugins]
                composeCompiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
                nativeBridge = { id = "love.yinlin.native-bridge", version.ref = "rachel" }
            """.trimIndent()
            catalog.writeText(original)
            val inputs = NativeCiInputs(repository)
            val identity = inputs.identity(NativeSourceCatalog(repository))
            catalog.writeText(original.replace("3.7.1", "3.7.2").replace("1.0.0", "1.1.0"))
            assertEquals(identity, inputs.identity(NativeSourceCatalog(repository)))
            catalog.writeText(original.replace("2.11.0", "2.12.0"))
            assertNotEquals(identity, inputs.identity(NativeSourceCatalog(repository)))
            catalog.writeText(original.replace("org.jetbrains.kotlin.plugin.compose", "example.compose.compiler"))
            assertNotEquals(identity, inputs.identity(NativeSourceCatalog(repository)))
            catalog.writeText(original.replace("love.yinlin.native-bridge", "example.native.bridge"))
            assertNotEquals(identity, inputs.identity(NativeSourceCatalog(repository)))
            catalog.writeText(original.replace("2.5.0-Beta2", "2.5.0"))
            assertNotEquals(identity, inputs.identity(NativeSourceCatalog(repository)))
        } finally { assertTrue(repository.deleteRecursively()) }
    }
}
