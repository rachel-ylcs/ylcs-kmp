package love.yinlin.gradle

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

    private var nativeBlock: KotlinNativeTarget.() -> Unit = {}
    fun native(block: KotlinNativeTarget.() -> Unit) {
        val previous = nativeBlock
        nativeBlock = {
            previous()
            block()
        }
    }
    internal fun KotlinNativeTarget.native() { nativeBlock(this) }

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
