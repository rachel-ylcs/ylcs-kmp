import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager

// Maintained library regressions remain separate from upstream's larger test suites.
val adaptationDirectory = project.buildFile.parentFile
val nativeDirectory = adaptationDirectory.parentFile.parentFile
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
    // Preserve the debug compiler mode and assertions; omit only CI's Apple debug map.
    if (konanTarget.family == Family.OSX && providers.environmentVariable("GITHUB_ACTIONS").orNull == "true") binary.linkerOpts("-S")
    testRuns.create("regression") {
        setExecutionSourceFrom(binary)
        if (HostManager.host == target.konanTarget) {
            val stageRuntime = tasks.register<Copy>("stage${target.name.replaceFirstChar(Char::uppercase)}RegressionTestRuntime") {
                from(nativeDirectory.resolve("vendor/icu/workspace/icudtl.dat"))
                into(project.provider { binary.outputFile.parentFile })
            }
            tasks.withType<KotlinNativeTest>().matching { it.name == "${target.name}RegressionTest" }.configureEach {
                dependsOn(stageRuntime)
            }
        }
    }
}
