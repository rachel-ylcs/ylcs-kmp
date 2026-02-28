package love.yinlin.project

import BuildEnvironment
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

class Constants(project: Project) {
    // 环境
    val environment = BuildEnvironment.Prod

    // 平台
    val platform = System.getProperty("os.name").lowercase().let {
        when {
            it.startsWith("windows") -> BuildPlatform.Windows
            it.startsWith("mac") -> BuildPlatform.Mac
            else -> BuildPlatform.Linux
        }
    }

    // 平台版本
    val platformVersion = System.getProperty("os.version") ?: ""

    // 架构
    val architecture = System.getProperty("os.arch").lowercase().let {
        when {
            it.startsWith("aarch64") -> BuildArchitecture.AARCH64
            it.startsWith("arm") -> BuildArchitecture.ARM
            // Windows x86_64
            it.startsWith("amd64") -> BuildArchitecture.X86_64
            // macOS Intel
            it.startsWith("x86_64") -> BuildArchitecture.X86_64
            else -> error("Unsupported architecture: $it")
        }
    }

    // 命名空间
    val namespace = "love.yinlin"

    // 资源标签
    val resourceTag = when (platform) {
        BuildPlatform.Windows -> "windows-x64"
        BuildPlatform.Linux -> "linux-x64"
        BuildPlatform.Mac -> if (architecture == BuildArchitecture.X86_64) "macos-x64" else "macos-arm64"
    }

    // Gradle版本
    val gradleVersion: String = project.gradle.gradleVersion

    // Java版本
    val javaVersion = System.getProperty("java.version") ?: ""

    // 最大内存
    val maxMemory = Runtime.getRuntime().maxMemory()

    // 已分配内存
    val totalMemory = Runtime.getRuntime().totalMemory()

    // 可用内存
    val freeMemory = Runtime.getRuntime().freeMemory()

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
        version = 350,
        versionName = "3.5.0",
        minVersion = 341,
        minVersionName = "3.4.1",
        description = "银临茶舍KMP跨平台APP",
        vendor = "银临茶舍",
        copyright = "© 2024-2025 银临茶舍 版权所有",
        homepage = "https://github.com/rachel-ylcs/ylcs-kmp",
        packageName = namespace
    )

    // JVM 配置
    val jvm = JvmConfig(
        version = 25,
        target = JvmTarget.JVM_25,
        androidTarget = JvmTarget.JVM_22,
        compatibility = JavaVersion.VERSION_22
    )

    // Android 配置
    val android = AndroidConfig(
        minSdk = 29,
        compileSdk = 36,
        targetSdk = 36,
        ndkAbi = arrayOf("arm64-v8a"),
        ndkVersion = "29.0.14206865",
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
        mainClass = "${app.packageName}.ModManagerKt"
    )

    // 混淆配置
    val proguard = ProguardConfig(
        version = "7.8.1",
        defaultRule = "proguard-android-optimize.txt"
    )

    // Host 配置
    val host = HostConfig(
        environment = environment,
        mainHost = "yinlin.love",
        mainPort = 1211,
        webServerPort = 8000
    )

    private val rootProject = project.rootProject

    // 目录树
    val root = RootProjectNode(rootProject.layout.projectDirectory, this)
}

data class Application(
    val name: String,
    val projectName: String,
    val displayName: String,
    val version: Int,
    val versionName: String,
    val minVersion: Int,
    val minVersionName: String,
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
    val androidTarget: JvmTarget,
    val compatibility: JavaVersion
)

class AndroidConfig(
    val minSdk: Int,
    val compileSdk: Int,
    val targetSdk: Int,
    val ndkAbi: Array<String>,
    val ndkVersion: String,
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
    val webServerPort: Int
) {
    val apiHost: String = "api.$mainHost"
    val apiUrl: String = when (environment) {
        BuildEnvironment.Dev -> "http://localhost:$mainPort"
        BuildEnvironment.Prod -> "https://$apiHost"
    }
}