package love.yinlin.gradle

import love.yinlin.gradle.task.BuildDesktopNativeTask
import love.yinlin.gradle.task.CopyDesktopNativeTask
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Usage
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.targets.native.internal.KotlinNativeDownloadTask

private const val NativeUsage = "rachel-desktop-native"

internal fun Project.registerDesktopNative(source: Directory, jarTaskName: String, additionalSources: List<String> = emptyList()) {
    if (isDokkaBuild) return
    val hasNativeSources = source.file("CMakeLists.txt").asFile.isFile && !source.file("native.ignore").asFile.exists()
    val toolchain = if (hasNativeSources && buildDefaults.platform == BuildPlatform.Windows) windowsDesktopNativeHome() else null
    val producer = tasks.register<BuildDesktopNativeTask>("buildDesktopNative") {
        inputDir.set(source)
        additionalSourceDirs.from(additionalSources.map(::file))
        if (toolchain != null) {
            val (home, prepare) = toolchain
            nativeHome.set(home)
            if (prepare != null) dependsOn(prepare)
        }
        onlyIf("CMake sources are present and are not ignored") { ensureNativeBuild(inputDir.get().asFile) }
    }
    tasks.matching { it.name == jarTaskName }.configureEach { dependsOn(producer) }

    val elements = configurations.create("rachelDesktopNativeElements") {
        isCanBeConsumed = true
        isCanBeResolved = false
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>(NativeUsage))
    }

    artifacts.add(elements.name, producer.flatMap { it.outputFile })
    tasks.withType<CopyDesktopNativeTask>().configureEach {
        nativeLibraries.from(this@registerDesktopNative.desktopNativeLibraries())
    }
}

private fun Project.windowsDesktopNativeHome(): Pair<Provider<Directory>, TaskProvider<*>?> {
    val taskName = "downloadKotlinNativeDistribution"
    if (taskName in tasks.names) {
        val download = tasks.named<KotlinNativeDownloadTask>(taskName)
        return download.flatMap { it.konanHome } to download
    }

    providers.gradleProperty("kotlin.native.home").orNull?.let { path ->
        val home = layout.projectDirectory.dir(path)
        return providers.provider { home } to null
    }

    // JVM-only projects have no Kotlin/Native target to register KGP's download task.
    val kotlin = (plugins.findPlugin("org.jetbrains.kotlin.multiplatform") ?: plugins.getPlugin("org.jetbrains.kotlin.jvm")) as KotlinBasePlugin
    val kotlinVersion = kotlin.pluginVersion
    val bundle = configurations.detachedConfiguration(dependencies.create("org.jetbrains.kotlin:kotlin-native-prebuilt:$kotlinVersion:windows-x86_64@zip"))
    bundle.isTransitive = false
    val archive = zipTree(bundle.elements.map { it.single().asFile })
    val prepare = tasks.register<Sync>("prepareDesktopKotlinNativeDistribution") {
        group = "native setup"
        description = "Prepare the Kotlin/Native toolchain for Windows JVM JNI sources"
        from(archive)
        into(layout.buildDirectory.dir("desktopNativeToolchain/$kotlinVersion"))
    }
    val home = layout.dir(prepare.map { it.destinationDir.resolve("kotlin-native-prebuilt-windows-x86_64-$kotlinVersion") })
    return home to prepare
}

@Suppress("UnstableApiUsage")
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
