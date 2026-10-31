package love.yinlin.gradle

import love.yinlin.gradle.task.PrepareDokkaCInteropTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

internal val Project.isDokkaBuild: Boolean get() = providers.gradleProperty("rachel.dokka").orNull == "true"

internal fun Project.configureDokkaCInterops(extension: KotlinMultiplatformExtension) {
    if (!isDokkaBuild) return
    extension.targets.withType<KotlinNativeTarget>().configureEach {
        val targetName = name.replaceFirstChar(Char::uppercase)
        compilations.getByName("main").cinterops.configureEach {
            val interopName = name
            val original = definitionFile.orNull ?: layout.projectDirectory.file("src/nativeInterop/cinterop/$interopName.def")
            val prepare = tasks.register<PrepareDokkaCInteropTask>("prepare${targetName}${interopName.replaceFirstChar(Char::uppercase)}DokkaInterop") {
                definitionFile.set(original)
                outputFile.set(layout.buildDirectory.file("dokka-cinterop/$targetName/$interopName/${original.asFile.name}"))
            }
            includeDirs(original.asFile.parentFile)
            definitionFile.set(prepare.flatMap { it.outputFile })
            tasks.named(interopProcessingTaskName).configure { dependsOn(prepare) }
        }
    }
}
