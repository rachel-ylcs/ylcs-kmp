package love.yinlin.nativebridge

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.HostManager

internal object NativeComposeTasks {
    fun configure(project: Project, kotlin: KotlinMultiplatformExtension) {
        for ((name, operation, buildType) in listOf(Triple("composeRun", "run", NativeBuildType.DEBUG), Triple("composeReleaseRun", "run", NativeBuildType.RELEASE), Triple("composeDistribution", "stage", NativeBuildType.RELEASE))) {
            val selected = project.provider {
                val target = kotlin.targets.withType(KotlinNativeTarget::class.java).singleOrNull { it.konanTarget == HostManager.host }
                    ?: throw GradleException("$name requires an enabled target for ${HostManager.host.name}")
                val binary = target.binaries.withType(Executable::class.java).singleOrNull { it.buildType == buildType && it.compilation.name == "main" }
                    ?: throw GradleException("$name requires exactly one ${buildType.name.lowercase()} main executable for ${target.name}")
                project.tasks.named(operation + target.name.replaceFirstChar(Char::uppercase) + binary.name.replaceFirstChar(Char::uppercase))
            }
            project.tasks.register(name) {
                group = "native application"
                description = if (operation == "stage") "Prepare the current host's complete Release application directory" else "Run the current host's ${buildType.name.lowercase()} Compose application"
                dependsOn(selected)
                if (operation == "stage") outputs.dir(selected.map { (it.get() as Sync).destinationDir })
            }
        }
    }
}
