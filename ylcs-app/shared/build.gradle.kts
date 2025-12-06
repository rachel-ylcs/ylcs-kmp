import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidLibrary1)
}

kotlin {
    C.useCompilerFeatures(this)

    android {
        namespace = "${C.app.packageName}.shared"
        compileSdk = C.android.compileSdk
        minSdk = C.android.minSdk
        lint.targetSdk = C.android.targetSdk

        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(C.jvm.androidTarget)
                }
            }
        }

        androidResources.enable = true
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
            testTask {
                enabled = false
            }
        }
        binaries.executable()
        binaries.library()
    }

    sourceSets {
        val commonMain by getting {
            useApi(
                projects.ylcsModule.compose.app,
                projects.ylcsModule.compose.screen,
                projects.ylcsModule.compose.service.all,
                projects.ylcsApp.cs,
            )
            useLib(
                projects.ylcsApp.mod,
                projects.ylcsModule.compose.component.all,
                projects.ylcsModule.compose.game,
                projects.ylcsModule.clientEngine,
                libs.compose.components.resources,
                libs.lottie,
                libs.lottie.network,
                libs.tool.html,
                libs.tool.blur,
                libs.tool.reorder,
                libs.tool.qrcode
            )
        }

        val jvmMain by creating {
            useSourceSet(commonMain)
            useLib(
                fileTree(mapOf("dir" to "libs/jvm", "include" to listOf("*.jar")))
            )
        }

        androidMain.configure {
            useSourceSet(jvmMain)
            useApi(
                libs.media3.ui,
                libs.media3.session,
                libs.media3.player,
            )
            useLib(
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
                fileTree(mapOf("dir" to "libs/desktop", "include" to listOf("*.jar")))
            )
        }

        wasmJsMain.configure {
            useSourceSet(commonMain)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "${C.app.packageName}.shared.resources"
}

composeCompiler {
    stabilityConfigurationFiles.add(C.root.config.stability)
    reportsDestination = C.root.shared.composeCompilerReport
    metricsDestination = C.root.shared.composeCompilerReport
}

afterEvaluate {
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
}