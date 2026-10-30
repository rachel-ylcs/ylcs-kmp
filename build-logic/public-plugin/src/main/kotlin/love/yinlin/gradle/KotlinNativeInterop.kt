package love.yinlin.gradle

import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

open class KotlinNativeInterop internal constructor() {
    internal val nativeSettings = NativeInterop()

    fun native(block: NativeInterop.() -> Unit) {
        nativeSettings.block()
    }

    internal fun configureNative(extension: KotlinMultiplatformExtension) {
        extension.targets.withType<KotlinNativeTarget>().configureEach {
            if (konanTarget.name in setOf("mingw_x64", "linux_x64", "macos_arm64")) nativeSettings.configure(this)
        }
    }
}
