import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

abstract class KotlinNativeExecutableTemplate : KotlinNativeTemplate() {
    open fun Executable.executable() { }
    open fun KotlinNativeTarget.native() { }

    override fun KotlinNativeTarget.baseNative() {
        binaries {
            executable {
                baseName = uniqueSafeName
                entryPoint = "main"

                executable()
            }
        }

        native()
    }
}