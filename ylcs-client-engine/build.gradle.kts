import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import kotlin.apply

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    C.useCompilerFeatures(this)

    androidTarget {
        C.jvmTarget(this)
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
        browser()
    }

    sourceSets {
        val commonMain by getting {
            useApi(
                projects.ylcsCsCore,
                libs.ktor.client,
                libs.ktor.client.negotiation,
                libs.ktor.client.websockets,
                libs.ktor.json,
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

        val jvmMain by creating {
            useSourceSet(commonMain)
            useLib(
                libs.ktor.okhttp,
            )
        }

        androidMain.configure {
            useSourceSet(jvmMain)
        }

        val desktopMain by getting {
            useSourceSet(jvmMain)
        }

        wasmJsMain.configure {
            useSourceSet(commonMain)
            useLib(
                libs.ktor.js,
            )
        }
    }
}

android {
    namespace = "${C.app.packageName}.client.engine"
    compileSdk = C.android.compileSdk

    defaultConfig {
        minSdk = C.android.minSdk
        lint.targetSdk = C.android.targetSdk
    }

    compileOptions {
        sourceCompatibility = C.jvm.compatibility
        targetCompatibility = C.jvm.compatibility
    }
}