package love.yinlin.gradle

import org.gradle.api.JavaVersion
import java.io.Serializable

/** Project-local template defaults. Repository and application data is supplied by consumers. */
class BuildDefaults : Serializable {
    var namespace: String = ""
    var stripFirstPathSegment: Boolean = false
    var projectVersion: Any? = null
    var languageFeatures: List<String> = emptyList()
    var resourceExcludes: List<String> = emptyList()
    var resourceClassPublic: Boolean = true
    var composeReports: Boolean = false
    var stabilityDirectory: String = "config"
    var jsTarget: String = "es2020"

    val jvm = Jvm()
    val android = Android()
    val ios = Ios()
    val desktop = Desktop()
    val native = Native()
    val publishing = Publishing()

    val platform: BuildPlatform get() = BuildHost.platform
    val architecture: BuildArchitecture get() = BuildHost.architecture

    fun jvm(block: Jvm.() -> Unit) { jvm.block() }
    fun android(block: Android.() -> Unit) { android.block() }
    fun ios(block: Ios.() -> Unit) { ios.block() }
    fun desktop(block: Desktop.() -> Unit) { desktop.block() }
    fun native(block: Native.() -> Unit) { native.block() }
    fun publishing(block: Publishing.() -> Unit) { publishing.block() }

    class Jvm : Serializable {
        var version: Int = 17
        var androidVersion: Int = 17
        var args: List<String> = emptyList()
        val target: String get() = version.toString()
        val androidTarget: String get() = androidVersion.toString()
        val compatibility: JavaVersion get() = JavaVersion.toVersion(version)
        val androidCompatibility: JavaVersion get() = JavaVersion.toVersion(androidVersion)
    }

    class Android : Serializable {
        var minSdk: Int = 21
        var compileSdk: Pair<Int, Int> = 35 to 0
        var targetSdk: Int = 35
        var ndkAbi: List<String> = emptyList()
        var ndkVersion: String = ""
        var proguardRule: String = "proguard-android-optimize.txt"
        fun compileSdk(major: Int, minor: Int = 0) { compileSdk = major to minor }
    }

    class Ios : Serializable {
        var target: String = "13.0"
        var linkUIKit: Boolean = false
        var disableKotlin250NativeCache: Boolean = false
    }

    class Desktop : Serializable {
        var proguardVersion: String? = null
        var packageVersion: String? = null
        var description: String? = null
        var copyright: String? = null
        var vendor: String? = null
        var licenseFile: String? = null
    }

    class Native : Serializable {
        var outputDirectory: String? = null
        var jniHeadersDirectory: String? = null
        var resourceTag: String? = null
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
