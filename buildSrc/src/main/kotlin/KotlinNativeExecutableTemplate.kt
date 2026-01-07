import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

abstract class KotlinNativeExecutableTemplate : KotlinNativeTemplate() {
    open fun Executable.executable() { }

    final override fun KotlinNativeTarget.native() {
        binaries {
            executable {
                baseName = libName
                entryPoint = "main"

                executable()
            }
        }
    }
}