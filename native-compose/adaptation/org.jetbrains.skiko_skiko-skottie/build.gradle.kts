import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
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
    extensions.configure<KotlinMultiplatformExtension> {
        if (supportNativeWindows) {
            context.configureNativeTarget(OS.Windows, Arch.X64, mingwX64()) { os, arch, isSimulator ->
                context.nativeSymbolSourcesFor(os, arch, isSimulator).also {
                    project.dependencies.add(it.name, project(":"))
                }
            }
        }
        sourceSets.getByName("commonMain").dependencies { api(project(":")) }
    }
}
apply(from = layout.projectDirectory.file("rachel-upstream.gradle.kts"))
