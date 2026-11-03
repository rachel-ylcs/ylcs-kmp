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
            interop {
                native {
                    cpp("shared") { compilerOpts("-DSHARED") }
                    windows { cpp("win32") }
                    linux { cpp("posix") }
                    macos { cpp("cocoa") }
                }
            }
            interop {
                native {
                    cpp("shared") { compilerOpts("-DSECOND") }
                    cpp("extra")
                }
            }
            interopSettings.configureNative(kotlin)

            for (target in targets) {
                with(target) {
                    val platformInterop = when (name) {
                        "windows" -> "win32"
                        "linux" -> "posix"
                        else -> "cocoa"
                    }
                    val interops = compilations.getByName("main").cinterops
                    assertEquals(setOf("shared", "extra", platformInterop), interops.names)
                    assertEquals(listOf("-DSHARED", "-DSECOND"), interops.getByName("shared").compilerOpts)
                    assertEquals(definition, interops.getByName("shared").definitionFile.get().asFile)
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
            interop {
                native {
                    cpp("existing") { packageName = "test.existing" }
                    cpp("extra")
                    cpp("extra")
                }
            }
            interopSettings.configureNative(project.extensions.getByType<KotlinMultiplatformExtension>())
            assertEquals(setOf("existing", "extra"), interops.names)
            assertSame(existing, interops.getByName("existing"))
            assertEquals(definition, existing.definitionFile.get().asFile)
            assertEquals("test.existing", existing.packageName)
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
                interop {
                    native {
                        cpp("shared")
                        cpp("win32")
                        windows { cpp("win32") }
                    }
                }
                with(target) {
                    baseNative()
                    interopSettings.configureNative(project.extensions.getByType<KotlinMultiplatformExtension>())
                    assertEquals(setOf("win32", "shared"), compilations.getByName("main").cinterops.names)
                    if (template is KotlinNativeExecutableTemplate) assertTrue(binaries.any { it is Executable })
                    if (template is KotlinNativeLibTemplate) assertTrue(binaries.any { it is SharedLibrary })
                }
            }
        }
    }

    @Test
    fun iosSourcesAccumulateAcrossInteropBlocks() {
        with(KotlinMultiplatformTemplate()) {
            interop { ios { cpp("src/nativeMain/cpp") } }
            interop {
                ios {
                    cpp("src/iosMain/cpp")
                    cpp("src/nativeMain/cpp")
                }
            }
            assertEquals(listOf("src/nativeMain/cpp", "src/iosMain/cpp"), interopSettings.iosSettings.sources.toList())
        }
    }

    @Test
    fun iosCppBuildIsDependencyOfCinteropAndCompilation() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(RachelPlugin::class.java)
        project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        val kotlin = project.extensions.getByType<KotlinMultiplatformExtension>()
        val target = kotlin.iosArm64()
        project.file("src/nativeMain/cpp").apply {
            mkdirs()
            resolve("CMakeLists.txt").writeText("cmake_minimum_required(VERSION 3.20)\nproject(test LANGUAGES CXX)\n")
        }
        val definition = project.file("src/nativeInterop/cinterop/test.def").apply {
            parentFile.mkdirs()
            writeText("headers = test.h\n")
        }
        val interop = target.compilations.getByName("main").cinterops.create("test") {
            definitionFile.set(definition)
        }

        project.registerNativeCpp(kotlin, listOf("src/nativeMain/cpp"))

        val producer = project.tasks.named("buildIosArm64Native").get()
        val cinteropTask = project.tasks.named(interop.interopProcessingTaskName).get()
        val compileTask = target.compilations.getByName("main").compileTaskProvider.get()
        assertTrue(producer in cinteropTask.taskDependencies.getDependencies(cinteropTask))
        assertTrue(producer in compileTask.taskDependencies.getDependencies(compileTask))
    }

    @Test
    fun desktopSourcesAccumulateAcrossInteropBlocks() {
        with(KotlinJvmTemplate()) {
            interop { desktop { cpp("src/nativeMain/cpp") } }
            interop {
                desktop {
                    cpp("src/nativeMain/cpp")
                    cpp("shared/cpp")
                }
            }
            assertEquals(listOf("src/nativeMain/cpp", "shared/cpp"), interopSettings.desktopSettings.sources.toList())
        }
        with(KotlinMultiplatformTemplate()) {
            interop { desktop { cpp("shared/cpp") } }
            interop { desktop { cpp("shared/extra") } }
            assertEquals(listOf("shared/cpp", "shared/extra"), interopSettings.desktopSettings.sources.toList())
        }
    }
}
