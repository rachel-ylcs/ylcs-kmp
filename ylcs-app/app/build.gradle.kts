import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidApplication)
}

kotlin {
    C.useCompilerFeatures(this)

    androidTarget {
        compilerOptions {
            jvmTarget.set(C.jvm.androidTarget)
        }
    }

    buildList {
        add(iosArm64())
        if (C.platform == BuildPlatform.Mac) {
            when (C.architecture) {
                BuildArchitecture.AARCH64 -> add(iosSimulatorArm64())
                BuildArchitecture.X86_64 -> add(iosX64())
                else -> {}
            }
        }
    }.forEach {
        if (C.platform == BuildPlatform.Mac) {
            it.compilations.getByName("main") {
                val nskeyvalueobserving by cinterops.creating
            }
        }
    }

    cocoapods {
        name = C.app.projectName
        version = C.app.versionName
        summary = C.app.description
        homepage = C.app.homepage
        ios.deploymentTarget = C.ios.target

        framework {
            baseName = C.app.projectName
            isStatic = true
        }

        if (C.platform == BuildPlatform.Mac) {
            pod("YLCSCore") {
                moduleName = "YLCSCore"
                extraOpts += listOf("-compiler-option", "-fmodules")
                source = path(C.root.iosApp.core.asFile)
            }
            pod("MMKV") {
                version = libs.versions.mmkv.get()
                extraOpts += listOf("-compiler-option", "-fmodules")
            }
            pod("MobileVLCKit") {
                version = libs.versions.vlcKit.get()
                extraOpts += listOf("-compiler-option", "-fmodules")
            }
            pod("SGQRCode") {
                version = libs.versions.sgQrcode.get()
                extraOpts += listOf("-compiler-option", "-fmodules")
            }
        }

        podfile = C.root.iosApp.podfile.asFile

        xcodeConfigurationToNativeBuildType["CUSTOM_DEBUG"] = NativeBuildType.DEBUG
        xcodeConfigurationToNativeBuildType["CUSTOM_RELEASE"] = NativeBuildType.RELEASE
    }

    jvm("desktop") {
        C.jvmTarget(this)
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "${C.app.projectName}.js"
                cssSupport {
                    enabled = true
                }
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    port = C.host.webServerPort
                    client?.overlay = false
                    proxy = mutableListOf(KotlinWebpackConfig.DevServer.Proxy(
                        context = mutableListOf(),
                        target = C.host.apiUrl,
                        secure = false
                    ))
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            useLib(
                projects.ylcsApp.cs,
                projects.ylcsApp.mod,
                projects.ylcsModule.compose.app,
                projects.ylcsModule.compose.screen,
                projects.ylcsModule.compose.component.all,
                projects.ylcsModule.compose.service.all,
                projects.ylcsModule.compose.shader,
                projects.ylcsModule.compose.animatedWebpDecoder,
                projects.ylcsModule.compose.game,
                projects.ylcsModule.clientEngine,
                libs.compose.components.resources,
                // lottie
                libs.lottie,
                libs.lottie.network,
                // tool
                libs.tool.html,
                libs.tool.blur,
                libs.tool.reorder,
                libs.tool.qrcode
            )
        }

        val jvmMain by creating {
            useSourceSet(commonMain)
            useLib(
                // local
                fileTree(mapOf("dir" to "libs/jvm", "include" to listOf("*.jar")))
            )
        }

        androidMain.configure {
            useSourceSet(jvmMain)
            useLib(
                libs.media3.ui,
                libs.media3.session,
                libs.media3.player,
                libs.mmkv.android,
                libs.scan.android,
                libs.scan.camera.android,

                fileTree(mapOf("dir" to "libs/android", "include" to listOf("*.aar", "*.jar")))
            )
        }

        val iosMain = iosMain.get().apply {
            useSourceSet(commonMain)
        }

        buildList {
            add(iosArm64Main)
            if (C.platform == BuildPlatform.Mac) {
                when (C.architecture) {
                    BuildArchitecture.AARCH64 -> add(iosSimulatorArm64Main)
                    BuildArchitecture.X86_64 -> add(iosX64Main)
                    else -> {}
                }
            }
        }.forEach {
            it.configure {
                useSourceSet(iosMain)
            }
        }

        val desktopMain by getting {
            useSourceSet(jvmMain)
            useLib(
                projects.ylcsModule.autoUpdate,
                projects.ylcsModule.singleInstance,
                fileTree(mapOf("dir" to "libs/desktop", "include" to listOf("*.jar")))
            )
        }

        wasmJsMain.configure {
            useSourceSet(commonMain)
        }
    }
}

composeCompiler {
    stabilityConfigurationFiles.add(C.root.config.stability)
    reportsDestination = C.root.app.composeCompilerReport
    metricsDestination = C.root.app.composeCompilerReport
}

compose.resources {
    packageOfResClass = "${C.app.packageName}.resources"
}

android {
    namespace = C.app.packageName
    compileSdk = C.android.compileSdk

    compileOptions {
        sourceCompatibility = C.jvm.compatibility
        targetCompatibility = C.jvm.compatibility
    }

    defaultConfig {
        applicationId = C.app.packageName
        minSdk = C.android.minSdk
        targetSdk = C.android.targetSdk
        versionCode = C.app.version
        versionName = C.app.versionName

        ndk {
            for (abi in C.android.ndkAbi) abiFilters += abi
        }
    }

    val androidSigningConfig = try {
        val localProperties = Properties().also { p ->
            C.root.localProperties.asFile.inputStream().use { p.load(it) }
        }
        val androidKeyName = localProperties.getProperty("androidKeyName")
        val androidKeyPassword = localProperties.getProperty("androidKeyPassword")
        signingConfigs {
            register(androidKeyName) {
                keyAlias = androidKeyName
                keyPassword = androidKeyPassword
                storeFile = C.root.config.androidKey.asFile
                storePassword = androidKeyPassword
            }
        }
        signingConfigs.getByName(androidKeyName)
    } catch (e: Throwable) {
        println("Can't load android signing config, error: ${e.message}")
        null
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            signingConfig = androidSigningConfig
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile(C.proguard.defaultRule),
                C.root.app.commonR8Rule, C.root.app.androidR8Rule
            )
            signingConfig = androidSigningConfig
        }
    }

    packaging {
        resources {
            excludes += C.excludes
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
        mainClass = C.app.mainClass

        if ("desktopRun" in currentTaskName) {
            val desktopWorkSpace = C.root.app.desktopWorkSpace.asFile
            desktopWorkSpace.mkdir()
            jvmArgs += arrayOf(
                "-Duser.dir=$desktopWorkSpace",
                "-Djava.library.path=${C.root.native.libs}",
                "--enable-native-access=ALL-UNNAMED"
            )
        }

        buildTypes.release.proguard {
            version = C.proguard.version
            isEnabled = true
            optimize = true
            obfuscate = true
            configurationFiles.from(C.root.app.commonR8Rule, C.root.app.desktopR8Rule)
        }

        nativeDistributions {
            packageName = C.app.name
            packageVersion = C.app.versionName
            description = C.app.description
            copyright = C.app.copyright
            vendor = C.app.vendor
            licenseFile.set(C.root.license)

            targetFormats(TargetFormat.Exe, TargetFormat.Deb, TargetFormat.Pkg)

            modules(*C.desktop.modules)

            windows {
                console = false
                exePackageVersion = C.app.versionName
                iconFile.set(C.root.config.icon)
            }

            linux {
                debPackageVersion = C.app.versionName
                iconFile.set(C.root.config.icon)
            }

            macOS {
                pkgPackageVersion = C.app.versionName
                iconFile.set(C.root.config.icon)
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
                from(C.root.app.androidOriginOutputDir.dir("release").file("app-release.apk"))
                into(C.root.outputs)
                rename { _ -> "[Android]${C.app.displayName}${C.app.versionName}.APK" }
            }
        }
    }

    // 发布安卓安装包
    val androidPublish by tasks.registering {
        dependsOn(assembleRelease)
        dependsOn(androidCopyAPK)
    }

    // 生成苹果版本号配置
    val appleGenVersionConfig by tasks.registering {
        val content = """
            BUNDLE_VERSION=${C.app.version}
            BUNDLE_SHORT_VERSION_STRING=${C.app.versionName}
        """.trimIndent()

        val configFile = C.root.iosApp.configurationFile.asFile
        outputs.file(configFile)
        outputs.upToDateWhen {
            configFile.takeIf { it.exists() }?.readText() == content
        }
        doLast {
            configFile.writeText(content)
        }
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
                from(C.root.app.desktopOriginOutput)
                into(C.root.outputs)
            }
            copy {
                from(C.root.native.libs)
                into(C.root.app.desktopLibOutput)
            }
            copy {
                from(C.root.config.currentPackages)
                into(C.root.app.desktopPackagesOutput)
            }
            val platformName = when (C.platform) {
                BuildPlatform.Windows -> "[Windows]"
                BuildPlatform.Linux -> "[Linux]"
                BuildPlatform.Mac -> "[MacOS]"
            }
            zip {
                from(C.root.outputs.dir(C.app.name).dir("app"))
                to(C.root.outputs.file("$platformName${C.app.displayName}${C.app.versionName}升级包.zip"))
            }
            zip {
                from(C.root.outputs.dir(C.app.name))
                to(C.root.outputs.file("$platformName${C.app.displayName}${C.app.versionName}.zip"))
            }
            delete {
                delete(C.root.outputs.dir(C.app.name))
            }
        }
    }

    // 发布桌面应用程序
    val desktopPublish by tasks.registering {
        dependsOn(createReleaseDistributable)
        dependsOn(desktopCopyDir)
    }

    val wasmJsBrowserDevelopmentRun = tasks.named("wasmJsBrowserDevelopmentRun")
    val wasmJsBrowserDistribution = tasks.named("wasmJsBrowserDistribution")

    val webCopyDir by tasks.registering {
        mustRunAfter(wasmJsBrowserDistribution)
        doLast {
            copy {
                from(C.root.app.webOriginOutput)
                into(C.root.app.webOutput)
            }
            delete {
                delete(*C.root.app.webOutput.asFile.listFiles {
                    it.extension == "map" || it.extension == "txt"
                }.map { C.root.app.webOutput.file(it.name) }.toTypedArray())
            }
            zip {
                from(C.root.app.webOutput)
                to(C.root.outputs.file("[Web]${C.app.displayName}${C.app.versionName}.zip"))
            }
            delete {
                delete(C.root.app.webOutput)
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