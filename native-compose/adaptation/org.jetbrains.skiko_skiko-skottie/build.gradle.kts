import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import tasks.configuration.configureNativeTarget
import tasks.configuration.nativeSymbolSourcesFor

plugins {
    kotlin("multiplatform")
    `maven-publish`
    signing
    id("org.gradle.crypto.checksum")
}

// Official Skottie supplies the shared bindings; register Windows before publication setup.
extensions.extraProperties["rachel.skiko.configure"] = { context: SkikoProjectContext ->
    project.group = "org.jetbrains.skiko"
    extensions.configure<KotlinMultiplatformExtension> {
        if (supportNativeWindows) {
            context.configureNativeTarget(OS.Windows, Arch.X64, mingwX64()) { os, arch, isSimulator ->
                context.nativeSymbolSourcesFor(os, arch, isSimulator).also {
                    project.dependencies.add(it.name, project.dependencies.project(mapOf("path" to ":")))
                }
            }
        }
        sourceSets.getByName("commonMain").dependencies { api(project(":")) }
    }
}
apply(from = layout.projectDirectory.file("rachel-upstream.gradle.kts"))

kotlin.targets.withType<KotlinNativeTarget>().configureEach {
    compilations.getByName("main").compileTaskProvider.configure {
        compilerOptions.moduleName.set("org.jetbrains.skiko:skiko-skottie")
    }
}

val nativeComposeDirectory = buildFile.parentFile.parentFile.parentFile
apply(from = nativeComposeDirectory.parentFile.resolve("build-logic/native-bridge/gradle/skiko-library-tests.gradle.kts"))
