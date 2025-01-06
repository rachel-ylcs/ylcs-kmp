plugins {
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.ktor) apply false
}

// Dir
val dirProject: Directory = layout.projectDirectory
val dirConfig by extra(dirProject.dir("config"))
val composeProjectName by extra("composeApp")
val dirComposeApp by extra(dirProject.dir(composeProjectName))
val dirIOSApp by extra(dirProject.dir("iosApp"))
val dirSrc by extra(dirComposeApp.dir("src"))
val dirBuild by extra(dirComposeApp.dir("build"))
val dirOutput by extra(dirProject.dir("outputs"))

// App
val appName by extra("ylcs")
val appVersion by extra(300)
val appVersionName by extra("3.0.0")
val appPackageName by extra("love.yinlin")
val appMainClass by extra("${appPackageName}.MainKt")

// Shared
val sharedDir by extra(dirProject.dir("shared").dir("src"))

// Common
val commonR8File by extra(dirComposeApp.file("R8Common.pro"))

// JVM
val composeStabilityFile by extra(dirConfig.file("stability.conf"))
val r8OptimizeFilename by extra("proguard-android-optimize.txt")

// Android
val androidDir by extra(dirSrc.dir("androidMain"))
val androidMinSDK by extra(29)
val androidBuildSDK by extra(35)
val androidNDKABI by extra(arrayOf("arm64-v8a"))
val androidKeyFile by extra(dirConfig.file("androidKey.jks"))
val androidKeyName by extra("rachel")
val androidKeyPassword by extra("rachel1211")
val androidR8File by extra(dirComposeApp.file("R8Android.pro"))
val androidOriginOutputPath by extra("${dirBuild}/outputs/apk/release/${composeProjectName}-release.apk")
val androidOutputDir by extra(dirOutput)
val androidOutputFileName by extra("ylcs.apk")

// IOS
val iosDir by extra(dirSrc.dir("iosArm64Main"))

// Desktop
val desktopDir by extra(dirSrc.dir("jvmMain"))
val desktopCurrentDir by extra(dirBuild.dir("desktopRun"))
val desktopLibsDir by extra(desktopDir.dir("libs"))
val desktopR8File by extra(dirComposeApp.file("R8Desktop.pro"))
val desktopOriginOutputPath by extra("${dirBuild}/compose/binaries/main-release/app/ylcs")
val desktopOutputDir by extra(dirOutput.dir("desktop"))
val desktopOutputAppDir by extra(desktopOutputDir.dir("app"))

// Web
val webDir by extra(dirSrc.dir("wasmJsMain"))
val webServerPort by extra(8000)
val webOriginOutputPath by extra("${dirBuild}/dist/wasmJs/productionExecutable")
val webOutputDir by extra(dirOutput.dir("web"))

// Server
val serverDir by extra(dirProject.dir("server"))
val serverBuildDir by extra(serverDir.dir("build"))
val serverOutputFileName by extra("ylcs.jar")
val serverOutputDir by extra(dirOutput)