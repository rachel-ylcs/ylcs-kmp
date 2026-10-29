package love.yinlin.gradle

import love.yinlin.nativebinary.NativeReleaseSymbols
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.SharedLibrary

open class KotlinNativeLibTemplate : KotlinNativeTemplate() {
    private var sharedLibBlock: SharedLibrary.() -> Unit = {}
    fun sharedLib(block: SharedLibrary.() -> Unit) {
        val previous = sharedLibBlock
        sharedLibBlock = {
            previous()
            block()
        }
    }
    internal fun SharedLibrary.sharedLib() { sharedLibBlock(this) }

    final override fun KotlinNativeTarget.baseNative() {
        binaries {
            sharedLib {
                baseName = uniqueSafeName

                sharedLib()
            }
        }

        native()
        NativeReleaseSymbols.configure(project, this)
    }
}
