package love.yinlin.nativebridge

import groovy.json.JsonSlurper
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File

/** Native tests need the same ICU data as an application; SDL and Skia remain statically linked. */
internal object SkikoTestRuntime {
    fun configure(project: Project, bridge: NativeBridgeExtension) {
        if (HostManager.host.name != "mingw_x64") return
        val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        val targets = kotlin.targets.withType(KotlinNativeTarget::class.java).filter { it.konanTarget == HostManager.host }
        if (targets.isEmpty()) return
        val extras = project.gradle.extensions.extraProperties
        val nativeDirectory = if (extras.has("love.yinlin.nativeCompose.root")) extras.get("love.yinlin.nativeCompose.root") as File else null
        val data = project.objects.fileCollection()
        if (nativeDirectory != null) {
            val state = JsonSlurper().parse(nativeDirectory.resolve("vendor/skiko/state.json")) as Map<*, *>
            val lock = JsonSlurper().parse(nativeDirectory.resolve("upstream/lock.json")) as Map<*, *>
            val skia = lock["skia"] as String
            val source = nativeDirectory.resolve(state["build"] as String)
            data.from(source.resolve("dependencies/skia/$skia/Skia-$skia-windows-Release-x64/out/Release-windows-x64/icudtl.dat"))
            data.builtBy(project.gradle.includedBuild("skiko").task(":unzipSkiaReleaseWindowsX64"))
        }
        else {
            val module = bridge.redirects.getByName("skiko-core").module.get().split(':')
            val runtime = project.configurations.create("skikoNativeTestRuntime") {
                isCanBeConsumed = false
                isCanBeResolved = true
                isTransitive = false
            }
            project.dependencies.add(runtime.name, "${module[0]}:${module[1]}-mingwx64:${module[2]}:icudtl@dat")
            data.from(runtime)
        }
        val tests = project.tasks.withType(KotlinNativeTest::class.java).matching { test -> targets.any { test.name.startsWith(it.name) } }
        // Register sibling tasks outside a task's lazy configuration action (Gradle 9 mutation guard).
        for (test in tests) {
            val stage = project.tasks.register("stage${test.name.replaceFirstChar(Char::uppercase)}SkikoRuntime", Copy::class.java) {
                group = "native setup"
                description = "Stage ICU data for ${test.name}"
                dependsOn(test.executableProperty)
                from(data) { rename { "icudtl.dat" } }
                into(project.provider { test.executable.parentFile })
            }
            test.dependsOn(stage)
        }
    }
}
