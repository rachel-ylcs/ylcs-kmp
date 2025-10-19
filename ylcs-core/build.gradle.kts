import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    C.useCompilerFeatures(this)

    androidTarget {
        C.jvmTarget(this)
        publishLibraryVariants("release")
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
                libs.kotlinx.atomicfu,
                libs.kotlinx.coroutines,
                libs.kotlinx.datetime,
                libs.kotlinx.io,
                libs.kotlinx.json,
            )
        }

        val nonAndroidMain by creating {
            useSourceSet(commonMain)
        }

        val nonWasmJsMain by creating {
            useSourceSet(commonMain)
        }

        val appleMain = appleMain.get().apply {
            useSourceSet(nonAndroidMain, nonWasmJsMain)
        }

        val jvmMain by creating {
            useSourceSet(nonWasmJsMain)
        }

        val iosMain = iosMain.get().apply {
            useSourceSet(appleMain)
        }

        androidMain.configure {
            useSourceSet(jvmMain)
            useApi(
                libs.kotlinx.coroutines.android
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

        val desktopMain by getting {
            useSourceSet(nonAndroidMain, jvmMain, appleMain)
            useApi(
                libs.kotlinx.coroutines.swing
            )
        }

        wasmJsMain.configure {
            useSourceSet(nonAndroidMain)
            useApi(
                libs.kotlinx.broswer
            )
        }
    }
}

android {
    namespace = "${C.app.packageName}.core"
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