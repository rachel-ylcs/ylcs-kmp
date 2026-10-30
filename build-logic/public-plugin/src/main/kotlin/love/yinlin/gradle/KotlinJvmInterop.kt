package love.yinlin.gradle

class KotlinJvmInterop internal constructor() {
    internal val desktopSettings = DesktopInterop()

    fun desktop(block: DesktopInterop.() -> Unit) {
        desktopSettings.block()
    }
}
