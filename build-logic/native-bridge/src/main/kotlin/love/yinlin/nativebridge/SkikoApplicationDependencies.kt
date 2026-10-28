package love.yinlin.nativebridge

import org.gradle.api.Project
import java.io.File

internal object SkikoApplicationDependencies {
    fun configure(project: Project, application: NativeApplicationExtension, bridge: NativeBridgeExtension) {
        val extras = project.gradle.extensions.extraProperties
        val nativeRoot = if (extras.has("love.yinlin.nativeCompose.root")) extras.get("love.yinlin.nativeCompose.root") as File else null
        if (application.skikoRuntimeEnabled.get()) configureSkiko(project, application, bridge, nativeRoot)
        if (!application.skikoWindowEnabled.get()) return
        val renderer = project.providers.gradleProperty("native.renderer").getOrElse("gpu")
        require(renderer in setOf("gpu", "software")) { "native.renderer must be gpu or software" }
        if (renderer == "software") application.arguments.add("--software")
    }

    private fun configureSkiko(project: Project, application: NativeApplicationExtension, bridge: NativeBridgeExtension, nativeRoot: File?) {
        if (nativeRoot != null) {
            for (target in NativeRuntimeAttributes.artifactSuffixes.keys) application.runtime(target) {
                files.from(nativeRoot.resolve("vendor/icu/workspace/text/icudtl.dat"))
            }
        }
        else {
            val module = bridge.redirects.getByName("skiko-core").module.get().split(':')
            for ((target, suffix) in NativeRuntimeAttributes.artifactSuffixes) application.runtime(target) {
                artifacts.put("${module[0]}:${module[1]}-$suffix:${module[2]}:icudtl-text@dat", "icudtl.dat")
            }
        }
    }
}
