import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import tasks.configuration.configureNativeTarget

// Applied by the prepared upstream Core project. Keep the maintained platform profile here.
val context = extensions.extraProperties["rachel.skiko.context"] as SkikoProjectContext
val adaptationDirectory = file(extensions.extraProperties["rachel.native.adaptation"] as String)

extensions.configure<KotlinMultiplatformExtension> {
    if (supportNativeWindows) context.configureNativeTarget(OS.Windows, Arch.X64, mingwX64())
    sourceSets.configureEach {
        val directory = adaptationDirectory.resolve("src/$name/kotlin")
        if (directory.isDirectory) kotlin.srcDir(directory)
    }
}

tasks.withType<CompileSkikoCppTask>().configureEach {
    val sourceSets = listOf("commonMain", "nativeMain", "nativeNativeJs", "nativeJsMain") +
        if (name.contains("Windows")) listOf("windowsMain") else if (name.contains("Linux")) listOf("linuxMain") else listOf("macosMain")
    for (sourceSet in sourceSets) {
        val directory = adaptationDirectory.resolve("src/$sourceSet/cpp")
        if (directory.isDirectory) {
            sourceRoots.add(layout.dir(provider { directory }))
            includeHeadersNonRecursive(directory)
        }
    }
}

tasks.withType<org.gradle.api.tasks.bundling.AbstractArchiveTask>().configureEach {
    if (name == "sourcesJar" || name.endsWith("SourcesJar")) {
        from(adaptationDirectory.resolve("src")) {
            include("**/*.cc", "**/*.cpp", "**/*.h", "**/*.hh")
            into("native-sources")
        }
    }
}
