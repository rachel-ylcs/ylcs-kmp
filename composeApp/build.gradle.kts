import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

val projectSrcDir: Directory get() = project.layout.projectDirectory.dir("src")
val projectBuildDir: Directory get() = project.layout.buildDirectory.get()
val outputDir: Directory get() = projectBuildDir.dir("production")
val androidMainDir: Directory get() = projectSrcDir.dir("androidMain")
val iosArm64MainDir: Directory get() = projectSrcDir.dir("iosArm64Main")
val desktopMainDir: Directory get() = projectSrcDir.dir("desktopMain")
val wasmJsMainDir: Directory get() = projectSrcDir.dir("wasmJsMain")

val proguardOptimizeFilename: String = "proguard-android-optimize.txt"

val androidKeyName: String = "rachel"
val androidKeyPassword: String = "rachel1211"
val androidKeyStoreFile: File get() = androidMainDir.file("key.jks").asFile
val androidR8RulesFile: File get() = androidMainDir.file("proguard-rules.pro").asFile
val androidOutputName: String = "ylcs.apk"

val desktopCurrentDir: Directory get() = projectBuildDir.dir("test")
val desktopLibsDir: Directory get() = desktopMainDir.dir("libs")
val desktopR8RulesFile: File get() = desktopMainDir.file("proguard-rules.pro").asFile
val desktopOutputDir: Directory get() = outputDir.dir("desktop")

val webOutputDir: Directory get() = outputDir.dir("web")

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidApplication)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    iosArm64 {
        binaries.framework {
            baseName = "composeApp"
            isStatic = true
        }
    }

    jvm("desktop") {
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
                outputFileName = "composeApp.js"
                cssSupport {
                    enabled = true
                }
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    port = 8000
                    static?.addAll(mutableListOf(rootDir.path, projectDir.path))
                    client?.overlay = false
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared)
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
            implementation(libs.ktor)
            implementation(libs.coil)
            implementation(libs.coil.network)
            implementation(libs.zoomImage)
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.activity.compose)
            implementation(libs.ktor.android)
            implementation(libs.mmkv.android)
        }

        iosArm64Main.dependencies {
            implementation(libs.ktor.apple)
        }

        wasmJsMain.dependencies {
            implementation(libs.ktor.web)
        }

        val desktopMain by getting
        desktopMain.dependencies {
            implementation(compose.preview)
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.java)
        }
    }
}

android {
    namespace = "love.yinlin"
    compileSdk = libs.versions.android.targetSdk.get().toInt()

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "love.yinlin"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = libs.versions.rachel.version.get().toInt()
        versionName = libs.versions.rachel.versionName.get()

        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += arrayOf("arm64-v8a")
        }
    }

    signingConfigs {
        register(androidKeyName) {
            keyAlias = androidKeyName
            keyPassword = androidKeyPassword
            storeFile = androidKeyStoreFile
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
                getDefaultProguardFile(proguardOptimizeFilename),
                androidR8RulesFile
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
        mainClass = "love.yinlin.MainKt"

        val taskName = project.gradle.startParameter.taskNames.firstOrNull() ?: ""
        if (taskName.endsWith("run") || taskName.endsWith("runRelease")) {
            jvmArgs += "-Duser.dir=${desktopCurrentDir}"
            jvmArgs += "-Djava.library.path=${desktopLibsDir}"
        }

        buildTypes.release.proguard {
            isEnabled = true
            optimize = true
            obfuscate = true
            joinOutputJars = true
            configurationFiles.from(desktopR8RulesFile)
        }

        nativeDistributions {
            val version = libs.versions.rachel.versionName.get()
            packageName = "ylcs"
            packageVersion = version
            includeAllModules = false

            targetFormats(TargetFormat.Exe)
            outputBaseDir.set(desktopOutputDir)

            windows {
                console = false
                exePackageVersion = version
            }
        }
    }
}

val publishAPK by tasks.registering(Copy::class) {
    from("${projectBuildDir}/outputs/apk/release/${project.name}-release.apk")
    into(outputDir)
    rename { _ -> androidOutputName }
}

val publishWeb by tasks.registering(Copy::class) {
    from("${projectBuildDir}/dist/wasmJs/productionExecutable")
    into(webOutputDir)
}

afterEvaluate {
    tasks.named("assembleRelease") {
        finalizedBy(publishAPK)
    }

    tasks.named("wasmJsBrowserDistribution") {
        finalizedBy(publishWeb)
    }
}