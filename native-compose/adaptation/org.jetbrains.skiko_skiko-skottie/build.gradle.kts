import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import tasks.configuration.configureNativeTarget
import tasks.configuration.nativeSymbolSourcesFor

plugins {
    kotlin("multiplatform")
    `maven-publish`
    signing
    id("org.gradle.crypto.checksum")
    id("org.jetbrains.dokka") apply false
}

val adaptationDirectory = buildFile.parentFile.canonicalFile
val dokkaBuild = providers.gradleProperty("rachel.dokka").orNull == "true"
if (dokkaBuild) layout.buildDirectory.set(adaptationDirectory.parentFile.parentFile.resolve("vendor/build/dokka/skottie"))

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

if (dokkaBuild) {
    apply(plugin = "org.jetbrains.dokka")
    extensions.configure<DokkaExtension> {
        modulePath.set("native-compose/skottie")
        dokkaPublications.named("html") { moduleName.set("Native Compose / Skottie") }
        dokkaSourceSets.configureEach {
            suppressedFiles.from(provider { sourceRoots.files.filter { !it.canonicalFile.toPath().startsWith(adaptationDirectory.toPath()) } })
        }
    }
}

kotlin.targets.withType<KotlinNativeTarget>().configureEach {
    compilations.getByName("main").compileTaskProvider.configure {
        compilerOptions.moduleName.set("org.jetbrains.skiko:skiko-skottie")
    }
}
