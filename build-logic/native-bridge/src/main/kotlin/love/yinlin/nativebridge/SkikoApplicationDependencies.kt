package love.yinlin.nativebridge

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
            for (target in NativeRuntimeAttributes.artifactSuffixes.keys) application.runtime(target) {
                files.from(nativeRoot.resolve("vendor/icu/workspace/icudtl.dat"))
                files.from(nativeRoot.resolve("vendor/icu/workspace/icu-LICENSE.txt"))
            }
        }
        else {
            val module = bridge.redirects.getByName("skiko-core").module.get().split(':')
            for ((target, suffix) in NativeRuntimeAttributes.artifactSuffixes) application.runtime(target) {
                artifacts.put("${module[0]}:${module[1]}-$suffix:${module[2]}:icudtl@dat", "icudtl.dat")
                artifacts.put("${module[0]}:${module[1]}-$suffix:${module[2]}:icu-license@txt", "icu-LICENSE.txt")
            }
        }
    }
}
