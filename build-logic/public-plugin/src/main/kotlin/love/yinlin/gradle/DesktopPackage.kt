package love.yinlin.gradle

import org.jetbrains.compose.desktop.application.dsl.JvmApplicationDistributions
import org.jetbrains.compose.desktop.application.dsl.JvmMacOSPlatformSettings
import org.jetbrains.compose.desktop.application.dsl.LinuxPlatformSettings
import org.jetbrains.compose.desktop.application.dsl.WindowsPlatformSettings

class DesktopPackage {
    var packageName: String? = null
    var mainClass: String? = null
    var jvmArgs: List<String> = emptyList()
    var jvmModules: List<String> = emptyList()
    var useAOT: Boolean = false

    internal var windowsSettings: (WindowsPlatformSettings.() -> Unit)? = null
    internal var linuxSettings: (LinuxPlatformSettings.() -> Unit)? = null
    internal var macosSettings: (JvmMacOSPlatformSettings.() -> Unit)? = null
    private var packageBlock: JvmApplicationDistributions.() -> Unit = {}

    fun windows(block: WindowsPlatformSettings.() -> Unit = {}) { windowsSettings = block }
    fun linux(block: LinuxPlatformSettings.() -> Unit = {}) { linuxSettings = block }
    fun macos(block: JvmMacOSPlatformSettings.() -> Unit = {}) { macosSettings = block }
    fun distribution(block: JvmApplicationDistributions.() -> Unit) { packageBlock = block }
    internal fun JvmApplicationDistributions.onPackage() { packageBlock(this) }
    internal fun usePlatformPackage(): Boolean = windowsSettings != null || linuxSettings != null || macosSettings != null
}
