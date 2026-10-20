package love.yinlin.gradle

import org.gradle.api.JavaVersion
import java.io.Serializable

@Suppress("unused")
class BuildDefaults : Serializable {
    var stripFirstPathSegment: Boolean = false

    /**
     * 命名空间
     */
    var namespace: String = ""

    /**
     * 组织
     */
    var organization: String = ""

    /**
     * 版权声明
     */
    var copyright: String = ""

    /**
     * 版本号值
     */
    var versionCode: Int? = null

    /**
     * 语言特性
     */
    var languageFeatures: List<String> = emptyList()

    /**
     * 排除资源
     */
    var resourceExcludes: List<String> = emptyList()

    /**
     * 生成 compose 稳定性报告
     */
    var composeReports: Boolean = false

    val jvm = Jvm()
    val android = Android()
    val ios = Ios()
    val desktop = Desktop()
    val web = Web()
    val native = Native()
    val publishing = Publishing()

    val platform: BuildPlatform get() = BuildHost.platform
    val architecture: BuildArchitecture get() = BuildHost.architecture
    val resourceTag: String get() = BuildHost.resourceTag

    fun jvm(block: Jvm.() -> Unit) { jvm.block() }
    fun android(block: Android.() -> Unit) { android.block() }
    fun ios(block: Ios.() -> Unit) { ios.block() }
    fun desktop(block: Desktop.() -> Unit) { desktop.block() }
    fun web(block: Web.() -> Unit) { web.block() }
    fun native(block: Native.() -> Unit) { native.block() }
    fun publishing(block: Publishing.() -> Unit) { publishing.block() }

    class Jvm : Serializable {
        var version: Int = 25
        var androidVersion: Int = 25
        var args: List<String> = emptyList()
        val target: String get() = version.toString()
        val androidTarget: String get() = androidVersion.toString()
        val compatibility: JavaVersion get() = JavaVersion.toVersion(version)
        val androidCompatibility: JavaVersion get() = JavaVersion.toVersion(androidVersion)
    }

    class Android : Serializable {
        var minSdk: Int = 26
        var compileSdk: Pair<Int, Int> = 37 to 1
        var targetSdk: Int = 37
        var ndkAbi: List<String> = emptyList()
        var ndkVersion: String = ""
        var proguardRule: String = "proguard-android-optimize.txt"
    }

    class Ios : Serializable {
        var target: String = "13.0"
        var linkUIKit: Boolean = false
    }

    class Desktop : Serializable {
        var proguardVersion: String? = null
        var packageVersion: String? = null
        var licenseFile: String? = null
    }

    class Web : Serializable {
        var webServerPort: Int = 8000
        var jsTarget: String = "es2020"
    }

    class Native : Serializable {
        var outputDirectory: String? = null
        var jniHeadersDirectory: String? = null
        var buildType: String = "Release"
        var parallelism: Int = 4
    }

    class Publishing : Serializable {
        var version: String? = null
        var central: Boolean = false
        var automaticRelease: Boolean = false
        var validateDeployment: Boolean = true
        var sign: Boolean = false
        var inceptionYear: String? = null
        var homepage: String? = null
        var licenseName: String? = null
        var licenseUrl: String? = null
        var developerId: String? = null
        var developerName: String? = null
        var scmConnection: String? = null
    }
}
