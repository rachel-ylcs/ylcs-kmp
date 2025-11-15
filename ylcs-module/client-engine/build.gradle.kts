import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import kotlin.apply

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary1)
}

kotlin {
    C.useCompilerFeatures(this)

    android {
        namespace = "${C.app.packageName}.module.client_engine"
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
    }

    iosArm64()
    if (C.platform == BuildPlatform.Mac) {
        when (C.architecture) {
            BuildArchitecture.AARCH64 -> iosSimulatorArm64()
            BuildArchitecture.X86_64 -> iosX64()
            else -> {}
        }
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
                projects.ylcsBase.csCore,
                libs.ktor.client,
            )
            useLib(
                libs.ktor.json,
                libs.ktor.client.negotiation,
                libs.ktor.client.websockets,
            )
        }

        val iosMain = iosMain.get().apply {
            useSourceSet(commonMain)
            useLib(
                libs.ktor.apple,
            )
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

        androidMain.configure {
            useSourceSet(commonMain)
            useApi(
                libs.ktor.okhttp,
            )
        }

        val desktopMain by getting {
            useSourceSet(commonMain)
            useApi(
                libs.ktor.okhttp,
            )
        }

        wasmJsMain.configure {
            useSourceSet(commonMain)
            useLib(
                libs.ktor.js,
            )
        }
    }
}