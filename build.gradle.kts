plugins {
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinCocoapods) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.kotlinAndroid) apply false
}

// Task
val taskName = project.gradle.startParameter.taskNames.firstOrNull() ?: ""
println("currentTaskName = $taskName")

// Env
enum class Environment { Dev, Prod }
val environment by extra(Environment.Prod)
val mainHost by extra("yinlin.love")
val apiHost by extra("api.$mainHost")
val apiBaseUrl by extra(when (environment) {
    Environment.Dev -> "http://localhost:1211"
    Environment.Prod -> "https://$apiHost"
})

// Dir
val dirProject: Directory = layout.projectDirectory
val dirConfig by extra(dirProject.dir("config"))
val dirPackages by extra(dirConfig.dir("packages"))
val appProjectName by extra(projects.ylcsApp.name)
val dirApp by extra(dirProject.dir(appProjectName))
val dirSrc by extra(dirApp.dir("src"))
val dirBuild by extra(dirApp.dir("build"))
val dirIOSApp by extra(dirProject.dir("iosApp"))
val dirNative by extra(dirProject.dir("native"))
val nativeLibsDir by extra(dirNative.dir("libs"))
val dirOutput by extra(dirProject.dir("outputs"))

// App
val appName by extra("ylcs")
val appDisplayName by extra("银临茶舍")
val appVersion by extra(311)
val appVersionName by extra("3.1.1")
val appPackageName by extra("love.yinlin")
val appMainClass by extra("${appPackageName}.MainKt")

// Common
val r8Dir by extra(dirApp.dir("proguard"))
val commonR8File by extra(r8Dir.file("R8Common.pro"))

// Desktop
val composeStabilityFile by extra(dirConfig.file("stability.conf"))
val r8OptimizeFilename by extra("proguard-android-optimize.txt")

// Android
val androidDir by extra(dirSrc.dir("androidMain"))
val androidMinSDK by extra(29)
val androidBuildSDK by extra(36)
val androidNDKABI by extra(arrayOf("arm64-v8a"))
val androidKeyFile by extra(dirConfig.file("androidKey.jks"))
val androidKeyName by extra("rachel")
val androidKeyPassword by extra("rachel1211")
val androidR8File by extra(r8Dir.file("R8Android.pro"))
val androidOriginOutputPath by extra("${dirBuild}/outputs/apk/release/${appProjectName}-release.apk")
val androidOutputDir by extra(dirOutput)
val androidOutputFileName by extra("ylcs.apk")

// IOS
val iosDir by extra(dirSrc.dir("iosMain"))

// Desktop
val desktopDir by extra(dirSrc.dir("desktopMain"))
val desktopCurrentDir by extra(dirBuild.dir("desktopRun"))
val desktopR8File by extra(r8Dir.file("R8Desktop.pro"))
val desktopOriginOutputPath by extra("${dirBuild}/compose/binaries/main-release/app")

// Web
val webDir by extra(dirSrc.dir("wasmJsMain"))
val webServerPort by extra(8000)
val webUseProxy by extra("webRun" in taskName) // 调试Web端时需要开启, 防止本地调试时出现跨域问题
val webOriginOutputPath by extra("${dirBuild}/dist/wasmJs/productionExecutable")
val webOutputDir by extra(dirOutput.dir("web"))

// Server
val serverDir by extra(dirProject.dir("ylcs-server"))
val serverBuildDir by extra(serverDir.dir("build"))
val serverCurrentDir by extra(serverBuildDir.dir("serverRun"))
val serverOutputFileName by extra("ylcs.jar")
val serverOutputDir by extra(dirOutput)