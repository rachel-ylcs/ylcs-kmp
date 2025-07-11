import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

enum class BuildEnvironment { Dev, Prod }

enum class BuildPlatform {
    Windows, Linux, Mac;

    override fun toString(): String = when (this) {
        Windows -> "win"
        Linux -> "linux"
        Mac -> "mac"
    }
}

enum class BuildArchitecture {
    X86_64, ARM, AARCH64;

    override fun toString(): String = when (this) {
        X86_64 -> "x86_64"
        ARM -> "arm"
        AARCH64 -> "aarch64"
    }
}

data class Application(
    val name: String,
    val projectName: String,
    val displayName: String,
    val version: Int,
    val versionName: String,
    val description: String,
    val vendor: String,
    val copyright: String,
    val homepage: String,
    val packageName: String,
) {
    val mainClass = "${packageName}.MainKt"
}

data class JvmConfig(
    val version: Int,
    val target: JvmTarget,
    val compatibility: JavaVersion
)

class AndroidConfig(
    val minSdk: Int,
    val compileSdk: Int,
    val targetSdk: Int,
    val ndkAbi: Array<String>,
    val outputName: String
)

data class IosConfig(
    val target: String
)

class DesktopConfig(
    val modules: Array<String>
)

data class ServerConfig(
    val outputName: String
)

data class ModManagerConfig(
    val name: String,
    val displayName: String,
    val mainClass: String
)

data class ProguardConfig(
    val version: String,
    val defaultRule: String,
)

class HostConfig(
    environment: BuildEnvironment,
    val mainHost: String,
    val mainPort: Int,
    val webUseProxy: Boolean,
    val webServerPort: Int
) {
    val apiHost: String = "api.$mainHost"
    val apiUrl: String = when (environment) {
        BuildEnvironment.Dev -> "http://localhost:$mainPort"
        BuildEnvironment.Prod -> "https://$apiHost"
    }
    val webServerUrl: String = "http://localhost:$webServerPort"
}

class Constants(project: Project) {
    private val rootProject = project.rootProject

    // 环境
    val environment = BuildEnvironment.Prod

    // 平台
    val platform = System.getProperty("os.name").let {
        when {
            it.lowercase().startsWith("windows") -> BuildPlatform.Windows
            it.lowercase().startsWith("mac") -> BuildPlatform.Mac
            else -> BuildPlatform.Linux
        }
    }

    // 架构
    val architecture = System.getProperty("os.arch").let {
        when {
            it.lowercase().startsWith("aarch64") -> BuildArchitecture.AARCH64
            it.lowercase().startsWith("arm") -> BuildArchitecture.ARM
            // Windows x86_64
            it.lowercase().startsWith("amd64") -> BuildArchitecture.X86_64
            // macOS Intel
            it.lowercase().startsWith("x86_64") -> BuildArchitecture.X86_64
            else -> error("Unsupported architecture: $it")
        }
    }

    // kotlin 特性
    val features = listOf(
        "-Xexpect-actual-classes",
        "-Xnested-type-aliases"
    )

    // 排除资源
    val excludes = listOf(
        "/META-INF/{AL2.0,LGPL2.1}",
        "DebugProbesKt.bin"
    )

    // APP
    val app = Application(
        name = "ylcs",
        projectName = "ylcs-app",
        displayName = "银临茶舍",
        version = 320,
        versionName = "3.2.0",
        description = "银临茶舍KMP跨平台APP",
        vendor = "银临茶舍",
        copyright = "© 2024-2025 银临茶舍 版权所有",
        homepage = "https://github.com/rachel-ylcs/ylcs-kmp",
        packageName = "love.yinlin"
    )

    // JVM 配置
    val jvm = JvmConfig(
        version = 21,
        target = JvmTarget.JVM_21,
        compatibility = JavaVersion.VERSION_21
    )

    // Android 配置
    val android = AndroidConfig(
        minSdk = 29,
        compileSdk = 36,
        targetSdk = 36,
        ndkAbi = arrayOf("arm64-v8a"),
        outputName = "ylcs.apk"
    )

    // iOS 配置
    val ios = IosConfig(
        target = "16.0"
    )

    // Desktop 配置
    val desktop = DesktopConfig(
        modules = arrayOf(
            "java.instrument",
            "java.net.http",
            "java.management",
            "jdk.unsupported",
        )
    )

    // Server 配置
    val server = ServerConfig(
        outputName = "ylcs.jar"
    )

    // ModManager 配置
    val modManager = ModManagerConfig(
        name = "ModManager",
        displayName = "银临茶舍Mod管理器",
        mainClass = "${app.packageName}.mod.ModManagerKt"
    )

    // 混淆配置
    val proguard = ProguardConfig(
        version = "7.7.0",
        defaultRule = "proguard-android-optimize.txt"
    )

    // Host 配置
    val host = HostConfig(
        environment = environment,
        mainHost = "yinlin.love",
        mainPort = 1211,
        webUseProxy = "webRun" in project.currentTaskName,
        webServerPort = 8000
    )

    // 目录树
    val root = RootProjectNode(this, rootProject.layout.projectDirectory)
}

val projectMap = mutableMapOf<Project, Constants>()
val Project.C: Constants get() = projectMap.getOrPut(this) { Constants(this) }