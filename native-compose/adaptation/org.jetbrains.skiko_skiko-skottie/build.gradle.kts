import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import tasks.configuration.configureNativeTarget
import tasks.configuration.nativeSymbolSourcesFor

// Official Skottie already supplies Native shared bindings; the missing Windows target is ours.
val context = extensions.extraProperties["rachel.skiko.context"] as SkikoProjectContext

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
