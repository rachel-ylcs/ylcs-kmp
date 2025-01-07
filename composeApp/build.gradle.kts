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

val appVersionName: String by rootProject.extra

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    composeCompiler {
        stabilityConfigurationFiles.add(rootProject.extra["composeStabilityFile"] as RegularFile)
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    iosArm64 {
        binaries.framework {
            baseName = rootProject.extra["composeProjectName"] as String
            isStatic = true
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        compilerOptions {
            sourceMap = false
        }
        browser {
            commonWebpackConfig {
                outputFileName = "${rootProject.extra["composeProjectName"]}.js"
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
        commonMain.dependencies {
            implementation(projects.shared)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.coroutines)
            implementation(libs.navigation)
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.json)
            implementation(libs.datetime)
            implementation(libs.ktor.client)
            implementation(libs.ktor.client.negotiation)
            implementation(libs.ktor.json)
            implementation(libs.sketch)
            implementation(libs.sketch.http)
            implementation(libs.sketch.resources)
            implementation(libs.sketch.gif)
            implementation(libs.sketch.webp)
            implementation(libs.sketch.zoom)
            implementation(libs.richtext)
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.activity.compose)
            implementation(libs.coroutines.android)
            implementation(libs.ktor.okhttp)
            implementation(libs.mmkv.android)
        }

        iosArm64Main.dependencies {
            implementation(libs.ktor.apple)
        }

        wasmJsMain.dependencies {
            implementation(libs.ktor.js)
        }

        jvmMain.dependencies {
            implementation(compose.preview)
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.okhttp)
        }
    }
}

android {
    namespace = rootProject.extra["appPackageName"] as String
    compileSdk = rootProject.extra["androidBuildSDK"] as Int

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
            signingConfig = signingConfigs.getByName(androidKeyName)
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
            jvmArgs += "-Djava.library.path=${rootProject.extra["desktopLibsDir"]}"
        }

        buildTypes.release.proguard {
            isEnabled = true
            optimize = true
            obfuscate = true
            configurationFiles.from(
                rootProject.extra["commonR8File"],
                rootProject.extra["desktopR8File"]
            )
        }

        nativeDistributions {
            packageName = rootProject.extra["appName"] as String
            packageVersion = appVersionName

            targetFormats(TargetFormat.Exe)

            modules(
                "java.instrument",
                "java.management",
                "jdk.unsupported",
                "jdk.unsupported",
                "java.net.http",
            )

            windows {
                console = false
                exePackageVersion = appVersionName
            }
        }
    }
}

// ----------------------------------------- 任务列表 ----------------------------------------------

afterEvaluate {
    val androidCopyAPK by tasks.registering(Copy::class) {
        from(rootProject.extra["androidOriginOutputPath"])
        into(rootProject.extra["androidOutputDir"]!!)
        rename { _ -> rootProject.extra["androidOutputFileName"] as String }
    }

    // 发布安卓安装包
    val androidPublish: Task by tasks.creating {
        dependsOn(tasks.named("assembleRelease"))
        finalizedBy(androidCopyAPK)
    }

    // 运行 桌面程序 Debug
    val desktopRunDebug by tasks.creating {
        dependsOn(tasks.named("run"))
    }

    // 运行 桌面程序 Release
    val desktopRunRelease by tasks.creating {
        dependsOn(tasks.named("runRelease"))
    }

    // 检查桌面模块完整性
    val desktopCheckModules by tasks.creating {
        dependsOn(tasks.named("suggestRuntimeModules"))
    }

    val desktopCopyLibs by tasks.registering(Copy::class) {
        from(rootProject.extra["desktopLibsDir"])
        into(rootProject.extra["desktopOutputAppDir"]!!)
    }

    val desktopCopyDir by tasks.registering(Copy::class) {
        from(rootProject.extra["desktopOriginOutputPath"])
        into(rootProject.extra["desktopOutputDir"]!!)
        finalizedBy(desktopCopyLibs)
    }

    // 发布桌面应用程序
    val desktopPublish by tasks.creating {
        dependsOn(tasks.named("createReleaseDistributable"))
        finalizedBy(desktopCopyDir)
    }

    val webCopyDir by tasks.registering(Copy::class) {
        from(rootProject.extra["webOriginOutputPath"])
        into(rootProject.extra["webOutputDir"]!!)
    }

    // 运行 Web 应用程序
    val webRun by tasks.creating {
        dependsOn(tasks.named("wasmJsBrowserRun"))
    }

    // 发布 Web 应用程序
    val webPublish: Task by tasks.creating {
        dependsOn(tasks.named("wasmJsBrowserDistribution"))
        finalizedBy(webCopyDir)
    }
}