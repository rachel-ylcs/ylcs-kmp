import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.SharedLibrary

abstract class KotlinNativeLibTemplate : KotlinNativeTemplate() {
    open fun SharedLibrary.sharedLib() { }

    final override fun KotlinNativeTarget.native() {
        binaries {
            sharedLib {
                baseName = libName
                linkerOpts += listOf(
                    "-O3",
                    "-DNDEBUG",
                    "-flto",
                    "-Wl,--gc-sections",
                    "-Wl,--strip-debug",
                    "-Wl,-s",
                )
                sharedLib()
            }
        }
    }
}