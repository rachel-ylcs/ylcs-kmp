import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinToolTask

allprojects {
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions.suppressWarnings.set(true)
    }
    tasks.withType<KotlinToolTask<*>>().configureEach {
        toolOptions.suppressWarnings.set(true)
    }
    tasks.withType<JavaCompile>().configureEach {
        options.isWarnings = false
    }
    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        extensions.getByType<KotlinMultiplatformExtension>().targets.withType<KotlinNativeTarget>().configureEach {
            compilations.configureEach {
                cinterops.configureEach { compilerOpts("-w") }
            }
        }
    }
}
