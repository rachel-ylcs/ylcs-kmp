package love.yinlin.gradle

import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.SharedLibrary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class KotlinNativeCppTest {
    @Test
    fun multiplatformMergesSharedAndPlatformInterops() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        val kotlin = project.extensions.getByType<KotlinMultiplatformExtension>()
        val targets = listOf(kotlin.mingwX64("windows"), kotlin.linuxX64("linux"), kotlin.macosArm64("macos"))
        val definition = project.file("src/nativeInterop/cinterop/shared.def")
        definition.parentFile.mkdirs()
        definition.writeText("language = C\n")

        with(KotlinMultiplatformTemplate()) {
            native { cpp = listOf("shared", "shared") }
            native { cpp = listOf("extra") }
            windows { cpp = listOf("win32", "shared") }
            linux { cpp = listOf("posix") }
            macos { cpp = listOf("cocoa") }

            for (target in targets) {
                with(target) {
                    when (name) {
                        "windows" -> windows()
                        "linux" -> linux()
                        "macos" -> macos()
                    }
                    native()
                    val platformInterop = when (name) {
                        "windows" -> "win32"
                        "linux" -> "posix"
                        else -> "cocoa"
                    }
                    assertEquals(setOf("shared", "extra", platformInterop), cpp.toSet())
                    assertEquals(definition, compilations.getByName("main").cinterops.getByName("shared").definitionFile.get().asFile)
                    assertTrue(compilations.getByName("test").cinterops.isEmpty())
                }
            }
        }
    }

    @Test
    fun existingInteropConfigurationIsPreserved() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        val target = project.extensions.getByType<KotlinMultiplatformExtension>().mingwX64("windows")
        val interops = target.compilations.getByName("main").cinterops
        val definition = project.file("custom/existing.def")
        val existing = interops.create("existing") { definitionFile.set(definition) }

        with(KotlinMultiplatformTemplate()) {
            target.cpp = listOf("existing", "extra", "extra")
            target.cpp = listOf("extra")
            assertEquals(setOf("existing", "extra"), target.cpp.toSet())
            assertSame(existing, interops.getByName("existing"))
            assertEquals(definition, existing.definitionFile.get().asFile)
        }
    }

    @Test
    fun nativeTemplatesApplySharedConfigurationAfterTheirBinarySetup() {
        for (template in listOf(KotlinNativeTemplate(), KotlinNativeExecutableTemplate(), KotlinNativeLibTemplate())) {
            val project = ProjectBuilder.builder().build()
            project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            val target = project.extensions.getByType<KotlinMultiplatformExtension>().mingwX64("windows")
            template.owner = project
            template.internalBackendInfo = BackendInfo("test.framework", ":fixture", project.projectDir.path)

            with(template) {
                windows { cpp = listOf("win32") }
                native { cpp = listOf("shared", "win32") }
                with(target) {
                    windows()
                    baseNative()
                    assertEquals(setOf("win32", "shared"), cpp.toSet())
                    if (template is KotlinNativeExecutableTemplate) assertTrue(binaries.any { it is Executable })
                    if (template is KotlinNativeLibTemplate) assertTrue(binaries.any { it is SharedLibrary })
                }
            }
        }
    }
}
