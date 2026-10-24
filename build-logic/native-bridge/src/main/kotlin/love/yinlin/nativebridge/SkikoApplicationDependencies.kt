package love.yinlin.nativebridge

import groovy.json.JsonSlurper
import org.gradle.api.Project
import java.io.File

/** Application data only: SDL bindings, platform adapters and static linkage belong to Skiko. */
internal object SkikoApplicationDependencies {
    fun configure(project: Project, application: NativeApplicationExtension, bridge: NativeBridgeExtension) {
        val extras = project.gradle.extensions.extraProperties
        val nativeRoot = if (extras.has("love.yinlin.nativeCompose.root")) extras.get("love.yinlin.nativeCompose.root") as File else null
        if (application.skikoRuntimeEnabled.get()) configureSkiko(project, application, bridge, nativeRoot)
        if (!application.skikoWindowEnabled.get()) return
        if (project.providers.gradleProperty("native.smoke").getOrElse("false").toBoolean()) application.arguments.add("--smoke")
        val renderer = project.providers.gradleProperty("native.renderer").getOrElse("gpu")
        require(renderer in setOf("gpu", "software")) { "native.renderer must be gpu or software" }
        if (renderer == "software") application.arguments.add("--software")
    }

    private fun configureSkiko(project: Project, application: NativeApplicationExtension, bridge: NativeBridgeExtension, nativeRoot: File?) {
        if (nativeRoot != null) {
            val state = JsonSlurper().parse(nativeRoot.resolve("vendor/skiko/state.json")) as Map<*, *>
            val lock = JsonSlurper().parse(nativeRoot.resolve("upstream/lock.json")) as Map<*, *>
            val skia = lock["skia"] as String
            val build = nativeRoot.resolve(state["build"] as String)
            application.runtime("mingw_x64") {
                files.from(build.resolve("dependencies/skia/$skia/Skia-$skia-windows-Release-x64/out/Release-windows-x64/icudtl.dat"))
                files.builtBy(project.gradle.includedBuild("skiko").task(":unzipSkiaReleaseWindowsX64"))
            }
        }
        else {
            val module = bridge.redirects.getByName("skiko-core").module.get().split(':')
            application.runtime("mingw_x64") {
                artifacts.put("${module[0]}:${module[1]}-mingwx64:${module[2]}:icudtl@dat", "icudtl.dat")
            }
        }
    }
}
