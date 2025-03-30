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

    composeCompiler {
        stabilityConfigurationFiles.add(rootProject.extra["composeStabilityFile"] as RegularFile)
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    iosArm64 {
        binaries.framework {
            baseName = rootProject.extra["composeProjectName"] as String
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
        val commonMain by getting {
            dependencies {
                implementation(projects.shared)
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
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

                implementation(libs.html)
                implementation(libs.uri)
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

        val appleMain = appleMain.get().apply {
            dependsOn(nonAndroidMain)
            dependsOn(nonWasmJsMain)
            dependencies {
                implementation(libs.ktor.apple)
            }
        }

        val jvmMain by creating {
            dependsOn(nonWasmJsMain)
            dependencies {
                implementation(libs.ktor.okhttp)
            }
        }

        androidMain.get().apply {
            dependsOn(jvmMain)
            dependencies {
                implementation(compose.preview)
                implementation(libs.compose.activity)
                implementation(libs.mmkv.android)
                implementation(libs.image.crop)
                implementation(libs.image.compress)
            }
        }

        val desktopMain by getting {
            when (desktopPlatform) {
                GradlePlatform.Windows -> {
                    val windowsMain by creating {
                        dependsOn(nonAndroidMain)
                        dependsOn(jvmMain)
                        dependencies {
                            val javafxClassifier = "win"
                            implementation(libs.javafx.base.get()) { artifact { classifier = javafxClassifier } }
                            implementation(libs.javafx.graphics.get()) { artifact { classifier = javafxClassifier } }
                            implementation(libs.javafx.controls.get()) { artifact { classifier = javafxClassifier } }
                            implementation(libs.javafx.fxml.get()) { artifact { classifier = javafxClassifier } }
                            implementation(libs.javafx.media.get()) { artifact { classifier = javafxClassifier } }
                            implementation(libs.javafx.swing.get()) { artifact { classifier = javafxClassifier } }
                            implementation(libs.javafx.web.get()) { artifact { classifier = javafxClassifier } }
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
                implementation(compose.desktop.currentOs)
            }
        }

        iosArm64Main.get().apply {
            dependsOn(appleMain)
            dependencies {

            }
        }

        wasmJsMain.get().apply {
            dependsOn(nonAndroidMain)
            dependencies {
                implementation(libs.ktor.js)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "${rootProject.extra["appPackageName"] as String}.resources"
}

dependencies {
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
            jvmArgs += "-Djava.library.path=${rootProject.extra["cppLibsDir"]}"
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
                "java.net.http",
                "jdk.jfr",
                "jdk.jsobject",
                "jdk.security.auth",
                "jdk.unsupported",
                "jdk.unsupported.desktop",
                "jdk.xml.dom"
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
                from(rootProject.extra["cppLibsDir"])
                into(rootProject.extra["desktopOutputAppDir"]!!)
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