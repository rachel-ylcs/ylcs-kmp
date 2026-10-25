import groovy.json.JsonSlurper
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.konan.target.HostManager

// Maintained library regressions remain separate from upstream's larger test suites.
val adaptationDirectory = project.buildFile.parentFile
val nativeDirectory = adaptationDirectory.parentFile.parentFile
val sourceLock = JsonSlurper().parse(nativeDirectory.resolve("upstream/lock.json")) as Map<*, *>
val kotlinExtension = extensions.getByType<KotlinMultiplatformExtension>()
val regressionRoot = kotlinExtension.sourceSets.maybeCreate("commonRegression")
val sharedTests = kotlinExtension.sourceSets.create("nativeRegressionTest") {
    dependsOn(regressionRoot)
    kotlin.setSrcDirs(listOf(adaptationDirectory.resolve("src/nativeTest/kotlin")))
    dependencies { implementation(kotlin("test")) }
}

kotlinExtension.targets.withType<KotlinNativeTargetWithHostTests>().configureEach {
    val target = this
    val regressions = compilations.create("regression") {
        defaultSourceSet.dependsOn(sharedTests)
        associateWith(target.compilations.getByName("main"))
    }
    binaries.test("regression", listOf(NativeBuildType.DEBUG)) { compilation = regressions }
    val binary = binaries.getTest("regression", NativeBuildType.DEBUG)
    testRuns.create("regression") {
        setExecutionSourceFrom(binary)
        if (target.konanTarget.name == "mingw_x64" && HostManager.host == target.konanTarget) {
            val stageRuntime = tasks.register<Copy>("stage${target.name.replaceFirstChar(Char::uppercase)}RegressionTestRuntime") {
                dependsOn(rootProject.tasks.named("unzipSkiaReleaseWindowsX64"))
                val skia = sourceLock["skia"] as String
                from(rootProject.file("dependencies/skia/$skia/Skia-$skia-windows-Release-x64/out/Release-windows-x64/icudtl.dat"))
                into(project.provider { binary.outputFile.parentFile })
            }
            tasks.withType<KotlinNativeTest>().matching { it.name == "${target.name}RegressionTest" }.configureEach {
                dependsOn(stageRuntime)
            }
        }
    }
}
