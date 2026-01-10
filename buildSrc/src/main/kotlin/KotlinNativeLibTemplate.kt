import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.SharedLibrary

abstract class KotlinNativeLibTemplate : KotlinNativeTemplate() {
    open fun SharedLibrary.sharedLib() { }
    open fun KotlinNativeTarget.native() { }

    final override fun KotlinNativeTarget.baseNative() {
        binaries {
            sharedLib {
                baseName = uniqueSafeName

                sharedLib()
            }
        }

        native()
    }
}