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

internal fun Project.registerNativeCpp(extension: KotlinMultiplatformExtension, iosSources: List<String> = emptyList()) {
    val source = layout.projectDirectory.dir("src/nativeMain/cpp").asFile
    val platformDirectories = listOf("windowsMain", "linuxMain", "macosMain").map { file("src/$it/cpp") }
    require(platformDirectories.none { it.isDirectory }) { "Place Native C/C++ sources in src/nativeMain/cpp and select platform sources in CMakeLists.txt" }
    val sharedSource = source.takeIf { it.resolve("CMakeLists.txt").isFile && !it.resolve("native.ignore").exists() }
    val iosSourceDirectories = iosSources.distinct().map { file(it) }.filter {
        it.resolve("CMakeLists.txt").isFile && !it.resolve("native.ignore").exists()
    }
    val desktopTargets = setOf("mingw_x64", "linux_x64", "macos_arm64")
    val iosTargets = setOf("ios_arm64", "ios_simulator_arm64", "ios_x64")

    extension.targets.withType<KotlinNativeTarget>().configureEach {
        val target = this
        val targetName = konanTarget.name
        val isDesktop = targetName in desktopTargets
        val isIos = targetName in iosTargets
        if (!isDesktop && !isIos) return@configureEach

        val compilation = compilations.getByName("main")
        val sources = when {
            isDesktop && compilation.allKotlinSourceSets.any { it.name == "nativeMain" } -> listOfNotNull(sharedSource)
            isIos -> iosSourceDirectories
            else -> emptyList()
        }
        if (sources.isEmpty()) return@configureEach

        compilation.cinterops.configureEach { sources.forEach { includeDirs(it) } }
        if (isDokkaBuild) return@configureEach

        // CI may restore these archives and exclude the CMake producer on another host.
        // Keep input paths independent of task outputs so Gradle can still snapshot them.
        val builds = sources.mapIndexed { index, cppSource ->
            val suffix = if (sources.size == 1) "" else "/$index"
            val nativeLibraries = layout.buildDirectory.dir("nativeCpp/${target.name}$suffix/lib")
            val taskSuffix = if (sources.size == 1) "" else "${index + 1}"
            val producer = tasks.register<BuildNativeCppTask>("build${target.name.replaceFirstChar(Char::uppercase)}Native$taskSuffix") {
                description = "Build C/C++ sources for the ${target.name} Kotlin/Native target"
                nativeTarget.set(targetName)
                inputDir.set(cppSource)
                inputDir.disallowChanges()
                val distribution = tasks.named<KotlinNativeDownloadTask>("downloadKotlinNativeDistribution")
                nativeHome.set(distribution.flatMap { it.konanHome })
                dependsOn(distribution)
                libraryDirectory.set(nativeLibraries)
                cmakeBuildDirectory.set(layout.buildDirectory.dir("nativeCppBuild/${target.name}$suffix"))
                if (isIos) cmakeOptions.add("CMAKE_OSX_DEPLOYMENT_TARGET" to buildDefaults.ios.target)
                onlyIf("The Kotlin/Native toolchain supports this target on the current host") { HostManager().isEnabled(target.konanTarget) }
            }
            producer to nativeLibraries
        }
        compilation.cinterops.configureEach {
            builds.forEachIndexed { index, (producer, nativeLibraries) ->
                extraOpts("-libraryPath", nativeLibraries.get().asFile.invariantSeparatorsPath)
                tasks.named(interopProcessingTaskName).configure {
                    dependsOn(producer)
                    inputs.files(nativeLibraries.map { it.asFileTree.matching { include("**/*.a", "**/*.lib") } })
                        .withPropertyName("nativeCppLibraries$index")
                        .withPathSensitivity(PathSensitivity.RELATIVE)
                }
            }
        }
        compilation.compileTaskProvider.configure { dependsOn(builds.map { it.first }) }
    }
}
