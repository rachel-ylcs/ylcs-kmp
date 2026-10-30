package love.yinlin.gradle

import love.yinlin.gradle.task.BuildNativeCppTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.native.internal.KotlinNativeDownloadTask
import org.jetbrains.kotlin.konan.target.HostManager

internal fun Project.registerNativeCpp(extension: KotlinMultiplatformExtension) {
    val source = layout.projectDirectory.dir("src/nativeMain/cpp").asFile
    val platformDirectories = listOf("windowsMain", "linuxMain", "macosMain").map { file("src/$it/cpp") }
    require(platformDirectories.none { it.isDirectory }) { "Place Native C/C++ sources in src/nativeMain/cpp and select platform sources in CMakeLists.txt" }
    if (!source.resolve("CMakeLists.txt").isFile || source.resolve("native.ignore").exists()) return
    extension.targets.withType<KotlinNativeTarget>().configureEach {
        val target = this
        if (konanTarget.name !in setOf("mingw_x64", "linux_x64", "macos_arm64")) return@configureEach
        val compilation = compilations.getByName("main")
        if (compilation.allKotlinSourceSets.none { it.name == "nativeMain" }) return@configureEach

        val producer = tasks.register<BuildNativeCppTask>("build${name.replaceFirstChar(Char::uppercase)}Native") {
            description = "Build C/C++ sources for the ${target.name} Kotlin/Native target"
            nativeTarget.set(target.konanTarget.name)
            inputDir.set(source)
            inputDir.disallowChanges()
            val distribution = tasks.named<KotlinNativeDownloadTask>("downloadKotlinNativeDistribution")
            nativeHome.set(distribution.flatMap { it.konanHome })
            dependsOn(distribution)
            libraryDirectory.set(layout.buildDirectory.dir("nativeCpp/${target.name}/lib"))
            cmakeBuildDirectory.set(layout.buildDirectory.dir("nativeCppBuild/${target.name}"))
            onlyIf("The Kotlin/Native toolchain supports this target on the current host") { HostManager().isEnabled(target.konanTarget) }
        }
        compilation.cinterops.configureEach {
            includeDirs(source)
            extraOpts("-libraryPath", producer.get().libraryDirectory.get().asFile.invariantSeparatorsPath)
            tasks.named(interopProcessingTaskName).configure { dependsOn(producer) }
        }
        compilation.compileTaskProvider.configure { dependsOn(producer) }
    }
}
