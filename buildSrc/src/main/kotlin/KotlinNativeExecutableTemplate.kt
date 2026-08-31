import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

abstract class KotlinNativeExecutableTemplate : KotlinNativeTemplate() {
    open val args: List<String> = emptyList()
    open fun Executable.executable() { }
    open fun KotlinNativeTarget.native() { }

    override fun KotlinNativeTarget.baseNative() {
        binaries {
            executable {
                baseName = uniqueSafeName
                entryPoint = "main"
                runTaskProvider?.get()?.setArgs(args)

                executable()
            }
        }

        native()
    }
}