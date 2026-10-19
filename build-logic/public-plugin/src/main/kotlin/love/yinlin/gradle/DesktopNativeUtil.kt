package love.yinlin.gradle

import love.yinlin.gradle.task.BuildDesktopNativeTask
import love.yinlin.gradle.task.CopyDesktopNativeTask
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Usage
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

private const val NativeUsage = "rachel-desktop-native"

internal fun Project.registerDesktopNative(source: Directory, jarTaskName: String) {
    val producer = tasks.register<BuildDesktopNativeTask>("buildDesktopNative") {
        inputDir.set(source)
        onlyIf("CMake sources are present and are not ignored") { ensureNativeBuild(inputDir.get().asFile) }
    }
    tasks.matching { it.name == jarTaskName }.configureEach { dependsOn(producer) }

    // Select these artifacts from the existing desktop dependency graph, rather than resolving every platform.
    val elements = configurations.create("rachelDesktopNativeElements") {
        isCanBeConsumed = true
        isCanBeResolved = false
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>(NativeUsage))
    }
    // An ignored/absent CMake source may still have a prebuilt library, as in the original copier.
    // Keeping the artifact provider also lets a file appear after project configuration finishes.
    artifacts.add(elements.name, producer.flatMap { it.outputFile })
    tasks.withType<CopyDesktopNativeTask>().configureEach {
        nativeLibraries.from(this@registerDesktopNative.desktopNativeLibraries())
    }
}

fun Project.desktopNativeLibraries(): FileCollection {
    val runtime = configurations.findByName("desktopRuntimeClasspath") ?: configurations.getByName("runtimeClasspath")
    val dependencies = runtime.incoming.artifactView {
        isLenient = true
        componentFilter { it is ProjectComponentIdentifier }
        withVariantReselection()
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>(NativeUsage))
    }.files
    val ownArtifacts = configurations.getByName("rachelDesktopNativeElements").artifacts.files
    return files(dependencies, ownArtifacts).filter { it.isFile }
}
