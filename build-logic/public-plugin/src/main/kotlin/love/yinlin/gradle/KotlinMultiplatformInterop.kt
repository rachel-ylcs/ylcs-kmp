package love.yinlin.gradle

class KotlinMultiplatformInterop internal constructor() : KotlinNativeInterop() {
    internal val desktopSettings = DesktopInterop()
    internal val iosSettings = IosInterop()

    fun desktop(block: DesktopInterop.() -> Unit) {
        desktopSettings.block()
    }

    fun ios(block: IosInterop.() -> Unit) {
        iosSettings.block()
    }
}
