import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidApplication)
}

val appName: String by rootProject.extra
val appVersionName: String by rootProject.extra

enum class GradlePlatform {
    Windows, Linux, Mac;
}

val desktopPlatform = System.getProperty("os.name").let { when {
    it.lowercase().startsWith("windows") -> GradlePlatform.Windows
    it.lowercase().startsWith("mac") -> GradlePlatform.Mac
    else -> GradlePlatform.Linux
} }

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    iosArm64 {
        binaries.framework {
            baseName = rootProject.extra["appProjectName"] as String
            isStatic = true
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        compilerOptions {
            sourceMap = false
        }
        browser {
            commonWebpackConfig {
                outputFileName = "${rootProject.extra["appProjectName"]}.js"
                cssSupport {
                    enabled = true
                }
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    port = rootProject.extra["webServerPort"] as Int
                    client?.overlay = false
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.ylcsShared)
                implementation(projects.ylcsMusic)
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.material3.icons)
                implementation(libs.compose.material3.iconsExtended)
                implementation(libs.compose.ui)
                implementation(libs.compose.ui.backhandler)
                implementation(libs.compose.components.uiToolingPreview)
                implementation(libs.compose.components.resources)

                implementation(libs.kotlinx.coroutines)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.io)
                implementation(libs.kotlinx.json)

                implementation(libs.runtime.navigation)
                implementation(libs.runtime.viewmodel)
                implementation(libs.runtime.lifecycle)

                implementation(libs.ktor.client)
                implementation(libs.ktor.client.negotiation)
                implementation(libs.ktor.json)

                implementation(libs.sketch)
                implementation(libs.sketch.http)
                implementation(libs.sketch.resources)
                implementation(libs.sketch.gif)
                implementation(libs.sketch.webp)
                implementation(libs.sketch.zoom)

                implementation(libs.tool.html)
                implementation(libs.tool.blur)
                implementation(libs.tool.reorder)
                implementation(libs.tool.scan)
            }
        }

        val nonAndroidMain by creating {
            dependsOn(commonMain)
            dependencies {

            }
        }

        val nonWasmJsMain by creating {
            dependsOn(commonMain)
            dependencies {

            }
        }

        val nonDesktopMain by creating {
            dependsOn(commonMain)
            dependencies {

            }
        }

        val appleMain = appleMain.get().apply {
            dependsOn(nonAndroidMain)
            dependsOn(nonWasmJsMain)
            dependencies {

            }
        }

        val jvmMain by creating {
            dependsOn(nonWasmJsMain)
            dependencies {
                implementation(libs.ktor.okhttp)

                implementation(fileTree(mapOf("dir" to "libs/jar/jvm", "include" to listOf("*.jar"))))
            }
        }

        androidMain.get().apply {
            dependsOn(jvmMain)
            dependsOn(nonDesktopMain)
            dependencies {
                implementation(compose.preview)
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.compose.activity)

                implementation(libs.media3.ui)
                implementation(libs.media3.session)
                implementation(libs.media3.player)

                implementation(libs.mmkv.android)

                implementation(fileTree(mapOf("dir" to "libs/jar/android", "include" to listOf("*.aar", "*.jar"))))
            }
        }

        val desktopMain by getting {
            when (desktopPlatform) {
                GradlePlatform.Windows -> {
                    val windowsMain by creating {
                        dependsOn(nonAndroidMain)
                        dependsOn(jvmMain)
                        dependencies {

                        }
                    }
                    dependsOn(windowsMain)
                }
                GradlePlatform.Linux -> {
                    val linuxMain by creating {
                        dependsOn(nonAndroidMain)
                        dependsOn(jvmMain)
                        dependencies {

                        }
                    }
                    dependsOn(linuxMain)
                }
                GradlePlatform.Mac -> {
                    val macMain by creating {
                        dependsOn(jvmMain)
                        dependsOn(appleMain)
                        dependencies {

                        }
                    }
                    dependsOn(macMain)
                }
            }

            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
                implementation(compose.desktop.currentOs)
                implementation(libs.vlcj)

                val javafxClassifier = when (desktopPlatform) {
                    GradlePlatform.Windows -> "win"
                    GradlePlatform.Linux -> "linux"
                    GradlePlatform.Mac -> "mac"
                }
                implementation(libs.javafx.base.get()) { artifact { classifier = javafxClassifier } }
                implementation(libs.javafx.graphics.get()) { artifact { classifier = javafxClassifier } }

                implementation(fileTree(mapOf("dir" to "libs/jar/desktop", "include" to listOf("*.jar"))))
            }
        }

        iosArm64Main.get().apply {
            dependsOn(appleMain)
            dependsOn(nonDesktopMain)
            dependencies {
                implementation(libs.ktor.apple)
            }
        }

        wasmJsMain.get().apply {
            dependsOn(nonAndroidMain)
            dependsOn(nonDesktopMain)
            dependencies {
                implementation(libs.ktor.js)
            }
        }
    }
}

composeCompiler {
    stabilityConfigurationFiles.add(rootProject.extra["composeStabilityFile"] as RegularFile)
    reportsDestination = layout.buildDirectory.dir("composeCompiler")
    metricsDestination = layout.buildDirectory.dir("composeCompiler")
}

compose.resources {
    packageOfResClass = "${rootProject.extra["appPackageName"] as String}.resources"
}

dependencies {
    implementation(libs.ui.graphics.android)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = rootProject.extra["appPackageName"] as String
    compileSdk = rootProject.extra["androidBuildSDK"] as Int

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    defaultConfig {
        applicationId = rootProject.extra["appPackageName"] as String
        minSdk = rootProject.extra["androidMinSDK"] as Int
        targetSdk = rootProject.extra["androidBuildSDK"] as Int
        versionCode = rootProject.extra["appVersion"] as Int
        versionName = appVersionName

        ndk {
            for (abi in rootProject.extra["androidNDKABI"] as Array<*>) {
                abiFilters += abi.toString()
            }
        }
    }

    val androidKeyName: String by rootProject.extra
    val androidKeyPassword: String by rootProject.extra

    signingConfigs {
        register(androidKeyName) {
            keyAlias = androidKeyName
            keyPassword = androidKeyPassword
            storeFile = (rootProject.extra["androidKeyFile"] as RegularFile).asFile
            storePassword = androidKeyPassword
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            signingConfig = signingConfigs.getByName(androidKeyName)
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile(rootProject.extra["r8OptimizeFilename"] as String),
                rootProject.extra["commonR8File"]!!,
                rootProject.extra["androidR8File"]!!
            )
            signingConfig = signingConfigs.getByName(androidKeyName)
        }
    }

    packaging {
        resources {
            excludes += arrayOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "DebugProbesKt.bin"
            )
        }

        dex {
            useLegacyPackaging = true
        }

        jniLibs {
            useLegacyPackaging = true
        }
    }
}

compose.desktop {
    application {
        mainClass = rootProject.extra["appMainClass"] as String

        // 为调试运行提供工作目录与库目录, 但发布打包时不需要
        val taskName = project.gradle.startParameter.taskNames.firstOrNull() ?: ""
        if (taskName.contains("desktopRun")) {
            jvmArgs += "-Duser.dir=${rootProject.extra["desktopCurrentDir"]}"
            jvmArgs += "-Djava.library.path=${rootProject.extra["cppLibsDir"]}"
        }

        buildTypes.release.proguard {
            version = "7.7.0"
            isEnabled = true
            optimize = true
            obfuscate = true
            configurationFiles.from(
                rootProject.extra["commonR8File"],
                rootProject.extra["desktopR8File"]
            )
        }

        nativeDistributions {
            packageName = appName
            packageVersion = appVersionName
            description = "银临茶舍KMP跨平台APP"
            copyright = "© 2024-2025 银临茶舍 版权所有"
            vendor = "银临茶舍"
            licenseFile.set(rootProject.file("LICENSE"))

            targetFormats(TargetFormat.Exe, TargetFormat.Deb, TargetFormat.Pkg)

            modules(
                "java.instrument",
                "java.net.http",
                "jdk.jfr",
                "jdk.unsupported",
            )

            val dirConfig: Directory by rootProject.extra

            windows {
                console = false
                exePackageVersion = appVersionName
                iconFile.set(dirConfig.file("icon.ico"))
            }

            linux {
                debPackageVersion = appVersionName
                iconFile.set(dirConfig.file("icon.png"))
            }

            macOS {
                pkgPackageVersion = appVersionName
                iconFile.set(dirConfig.file("icon.icns"))
            }
        }
    }
}

// ----------------------------------------- 任务列表 ----------------------------------------------

afterEvaluate {
    val assembleRelease = tasks.named("assembleRelease")

    val androidCopyAPK by tasks.registering {
        mustRunAfter(assembleRelease)
        doLast {
            copy {
                from(rootProject.extra["androidOriginOutputPath"])
                into(rootProject.extra["androidOutputDir"]!!)
                rename { _ -> rootProject.extra["androidOutputFileName"] as String }
            }
        }
    }

    // 发布安卓安装包
    val androidPublish by tasks.registering {
        dependsOn(assembleRelease)
        dependsOn(androidCopyAPK)
    }

    val run = tasks.named("run")
    val runRelease = tasks.named("runRelease")
    val createReleaseDistributable = tasks.named("createReleaseDistributable")
    val suggestRuntimeModules = tasks.named("suggestRuntimeModules")

    // 运行 桌面程序 Debug
    val desktopRunDebug by tasks.registering {
        dependsOn(run)
    }

    // 运行 桌面程序 Release
    val desktopRunRelease by tasks.registering {
        dependsOn(runRelease)
    }

    // 检查桌面模块完整性
    val desktopCheckModules by tasks.registering {
        dependsOn(suggestRuntimeModules)
    }

    val desktopCopyDir by tasks.registering {
        mustRunAfter(createReleaseDistributable)
        doLast {
            copy {
                from(rootProject.extra["desktopOriginOutputPath"])
                into(rootProject.extra["desktopOutputDir"]!!)
            }
        }
    }

    val desktopCopyLibs by tasks.registering {
        mustRunAfter(desktopCopyDir)
        doLast {
            copy {
                val desktopOutputDir: Directory by rootProject.extra
                val outputAppLibDir = desktopOutputDir.let {
                    when (desktopPlatform) {
                        GradlePlatform.Mac -> it.dir("$appName.app/Contents/app")
                        else -> it.dir("$appName/app")
                    }
                }
                from(rootProject.extra["cppLibsDir"])
                into(outputAppLibDir)
            }
        }
    }

    // 发布桌面应用程序
    val desktopPublish by tasks.registering {
        dependsOn(createReleaseDistributable)
        dependsOn(desktopCopyDir)
        dependsOn(desktopCopyLibs)
    }

    val wasmJsBrowserDevelopmentRun = tasks.named("wasmJsBrowserDevelopmentRun")
    val wasmJsBrowserDistribution = tasks.named("wasmJsBrowserDistribution")

    val webCopyDir by tasks.registering {
        mustRunAfter(wasmJsBrowserDistribution)
        doLast {
            copy {
                from(rootProject.extra["webOriginOutputPath"])
                into(rootProject.extra["webOutputDir"]!!)
            }
        }
    }

    // 运行 Web 应用程序
    val webRun by tasks.registering {
        dependsOn(wasmJsBrowserDevelopmentRun)
    }

    // 发布 Web 应用程序
    val webPublish by tasks.registering {
        dependsOn(wasmJsBrowserDistribution)
        dependsOn(webCopyDir)
    }
}