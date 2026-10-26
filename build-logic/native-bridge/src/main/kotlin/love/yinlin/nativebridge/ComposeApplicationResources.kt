package love.yinlin.nativebridge

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractExecutable
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

/** Deploys the official Compose plugin's resolved tree, including resources from dependent modules. */
internal object ComposeApplicationResources {
    fun configure(project: Project, kotlin: KotlinMultiplatformExtension, applicationConfigured: Boolean) {
        kotlin.targets.withType(KotlinNativeTarget::class.java).configureEach {
            val target = this
            if (target.konanTarget.name in setOf("mingw_x64", "linux_x64", "macos_arm64")) {
                target.binaries.withType(AbstractExecutable::class.java).configureEach {
                    if (applicationConfigured && this is Executable) return@configureEach
                    val suffix = target.name.replaceFirstChar(Char::uppercase) + name.replaceFirstChar(Char::uppercase)
                    register(project, target, this, suffix)
                }
            }
        }
    }

    fun register(project: Project, target: KotlinNativeTarget, binary: AbstractExecutable, suffix: String): TaskProvider<SyncComposeResources>? {
        if (!project.plugins.hasPlugin("org.jetbrains.compose")) return null
        val resources = listOf(target.compilations.getByName("main").defaultSourceSet.resources, binary.compilation.defaultSourceSet.resources).distinct()
        val copy = project.tasks.register("copy${suffix}ComposeResources", SyncComposeResources::class.java) {
            group = "native application"
            description = "Copy resolved Compose resources beside ${target.name} ${binary.name}"
            dependsOn(resources)
            resourceDirectories.from(project.provider { resources.flatMap { it.srcDirs }.distinct().map { it.resolve("composeResources") } })
            outputDirectory.set(project.layout.dir(project.provider { binary.outputFile.parentFile.resolve("composeResources") }))
        }
        binary.linkTaskProvider.configure { dependsOn(copy) }
        return copy
    }
}
