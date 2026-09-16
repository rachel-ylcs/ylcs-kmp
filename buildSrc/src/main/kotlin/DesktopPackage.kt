import org.gradle.api.internal.catalog.DelegatingProjectDependency
import org.jetbrains.compose.desktop.application.dsl.JvmApplicationDistributions
import org.jetbrains.compose.desktop.application.dsl.JvmMacOSPlatformSettings
import org.jetbrains.compose.desktop.application.dsl.LinuxPlatformSettings
import org.jetbrains.compose.desktop.application.dsl.WindowsPlatformSettings

abstract class DesktopPackage {
    fun interface Windows {
        fun onSettings(settings: WindowsPlatformSettings)
    }

    fun interface Linux {
        fun onSettings(settings: LinuxPlatformSettings)
    }

    fun interface MacOS {
        fun onSettings(settings: JvmMacOSPlatformSettings)
    }

    abstract val packageName: String // 包名
    abstract val mainClass: String // 主类
    open val jvmArgs: List<String> = emptyList() // JVM参数
    open val jvmModules: List<String> = emptyList() // JVM模块
    open val proguard: List<DelegatingProjectDependency> = emptyList() // 需要处理混淆规则的依赖模块
    open val useAOT: Boolean = false // 使用AOT

    open fun JvmApplicationDistributions.onPackage() { }

    internal fun usePlatformPackage(): Boolean = this is Windows || this is Linux || this is MacOS
}