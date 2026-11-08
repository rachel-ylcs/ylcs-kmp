package love.yinlin.gradle

import love.yinlin.gradle.task.BuildNativeCppTask
import org.gradle.api.Project
import org.gradle.api.tasks.PathSensitivity
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.native.internal.KotlinNativeDownloadTask
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File

internal fun Project.registerNativeCpp(extension: KotlinMultiplatformExtension) {
    val source = layout.projectDirectory.dir("src/nativeMain/cpp").asFile
    val platformDirectories = listOf("windowsMain", "linuxMain", "macosMain").map { file("src/$it/cpp") }
    require(platformDirectories.none { it.isDirectory }) { "Place Native C/C++ sources in src/nativeMain/cpp and select platform sources in CMakeLists.txt" }
    if (!source.resolve("CMakeLists.txt").isFile || source.resolve("native.ignore").exists()) return
    extension.targets.withType<KotlinNativeTarget>().configureEach {
        if (konanTarget.name !in setOf("mingw_x64", "linux_x64", "macos_arm64")) return@configureEach
        val compilation = compilations.getByName("main")
        if (compilation.allKotlinSourceSets.none { it.name == "nativeMain" }) return@configureEach
        registerCmakeCpp(this, source)
    }
}

internal fun Project.registerIosCpp(target: KotlinNativeTarget, sources: Set<String>) {
    val source = file("src/iosMain/cpp")
    if (!source.resolve("CMakeLists.txt").isFile || source.resolve("native.ignore").exists()) return
    registerCmakeCpp(target, source, sources.map(::file)) {
        cmakeOptions.add("CMAKE_OSX_DEPLOYMENT_TARGET" to buildDefaults.ios.target)
    }
}

private fun Project.registerCmakeCpp(target: KotlinNativeTarget, source: File, sharedSources: List<File> = emptyList(), configure: BuildNativeCppTask.() -> Unit = {}) {
    val compilation = target.compilations.getByName("main")
    compilation.cinterops.configureEach {
        includeDirs(source, *sharedSources.toTypedArray())
    }
    if (isDokkaBuild) return

    // Keep the input paths independent of task outputs so CI can restore the archives on another host.
    val nativeLibraries = layout.buildDirectory.dir("nativeCpp/${target.name}/lib")
    val producer = tasks.register<BuildNativeCppTask>("build${target.name.replaceFirstChar(Char::uppercase)}Native") {
        description = "Build C/C++ sources for the ${target.name} Kotlin/Native target"
        nativeTarget.set(target.konanTarget.name)
        inputDir.set(source)
        inputDir.disallowChanges()
        if (sharedSources.isNotEmpty()) {
            extraSourceFiles.from(sharedSources.map { fileTree(it) { exclude("**/.git", "**/.git/**", "**/build/**") } })
            cmakeOptions.add("RACHEL_CPP_SOURCE_DIRS" to sharedSources.joinToString(";") { it.invariantSeparatorsPath })
        }
        val distribution = tasks.named<KotlinNativeDownloadTask>("downloadKotlinNativeDistribution")
        nativeHome.set(distribution.flatMap { it.konanHome })
        dependsOn(distribution)
        libraryDirectory.set(nativeLibraries)
        cmakeBuildDirectory.set(layout.buildDirectory.dir("nativeCppBuild/${target.name}"))
        onlyIf("The Kotlin/Native toolchain supports this target on the current host") { HostManager().isEnabled(target.konanTarget) }
        configure()
    }
    compilation.cinterops.configureEach {
        extraOpts("-libraryPath", nativeLibraries.get().asFile.invariantSeparatorsPath)
        tasks.named(interopProcessingTaskName).configure {
            dependsOn(producer)
            inputs.files(nativeLibraries.map { it.asFileTree.matching { include("**/*.a", "**/*.lib") } })
                .withPropertyName("nativeCppLibraries")
                .withPathSensitivity(PathSensitivity.RELATIVE)
        }
    }
    compilation.compileTaskProvider.configure { dependsOn(producer) }
}
