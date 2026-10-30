package love.yinlin.gradle

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultCInteropSettings

class NativeInterop internal constructor() {
    private val actions = mutableListOf<KotlinNativeTarget.() -> Unit>()

    /**
     * 注册 main compilation 的 CInterop，
     *
     * 默认读取 src/nativeInterop/cinterop/<name>.def
     *
     * 同名配置按声明顺序合并
     */
    fun cpp(name: String, block: DefaultCInteropSettings.() -> Unit = {}) {
        actions += {
            compilations.getByName("main").cinterops.maybeCreate(name).apply(block)
        }
    }

    fun windows(block: NativeInterop.() -> Unit) = platform("mingw_x64", block)

    fun linux(block: NativeInterop.() -> Unit) = platform("linux_x64", block)

    fun macos(block: NativeInterop.() -> Unit) = platform("macos_arm64", block)

    private fun platform(target: String, block: NativeInterop.() -> Unit) {
        val settings = NativeInterop().apply(block)
        actions += {
            if (konanTarget.name == target) settings.configure(this)
        }
    }

    internal fun configure(target: KotlinNativeTarget) {
        for (action in actions) target.action()
    }
}
