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
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.activity.compose)
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
                rootProject.extra["androidR8File"] as RegularFile
            )
            signingConfig = signingConfigs.getByName(androidKeyName)
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
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

        val taskName = project.gradle.startParameter.taskNames.firstOrNull() ?: ""
        if (taskName.endsWith("run") || taskName.endsWith("runRelease")) {
            jvmArgs += "-Duser.dir=${rootProject.extra["desktopCurrentDir"]}"
            jvmArgs += "-Djava.library.path=${rootProject.extra["desktopLibsDir"]}"
        }

        buildTypes.release.proguard {
            isEnabled = true
            optimize = true
            obfuscate = true
            joinOutputJars = true
            configurationFiles.from(rootProject.extra["desktopR8File"])
        }

        nativeDistributions {
            packageName = rootProject.extra["appName"] as String
            packageVersion = appVersionName
            includeAllModules = false

            targetFormats(TargetFormat.Exe)
            outputBaseDir.set(rootProject.extra["desktopOutputDir"] as Directory)

            windows {
                console = false
                exePackageVersion = appVersionName
            }
        }
    }
}

val publishAPK by tasks.registering(Copy::class) {
    from(rootProject.extra["androidOriginOutputPath"])
    into(rootProject.extra["androidOutputDir"] as Directory)
    rename { _ -> rootProject.extra["androidOutputFileName"] as String }
}

val publishWeb by tasks.registering(Copy::class) {
    from(rootProject.extra["webOriginOutputPath"])
    into(rootProject.extra["webOutputDir"] as String)
}

afterEvaluate {
    tasks.named("assembleRelease") {
        finalizedBy(publishAPK)
    }

    tasks.named("wasmJsBrowserDistribution") {
        finalizedBy(publishWeb)
    }
}