import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary1)
}

kotlin {
    C.useCompilerFeatures(this)

    android {
        namespace = "${C.app.packageName}.base.core"
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
                libs.kotlinx.atomicfu,
                libs.kotlinx.coroutines,
                libs.kotlinx.datetime,
                libs.kotlinx.io,
                libs.kotlinx.json,
            )
        }

        val skikoMain by creating {
            useSourceSet(commonMain)
        }

        val clientMain by creating {
            useSourceSet(commonMain)
        }

        val appleMain = appleMain.get().apply {
            useSourceSet(skikoMain, clientMain)
        }

        val jvmMain by creating {
            useSourceSet(clientMain)
        }

        androidMain.configure {
            useSourceSet(jvmMain)
            useApi(
                libs.kotlinx.coroutines.android
            )
        }

        val iosMain = iosMain.get().apply {
            useSourceSet(appleMain)
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
            useSourceSet(skikoMain, jvmMain)
            if (C.platform == BuildPlatform.Mac) {
                useSourceSet(appleMain)
            }
            useApi(
                libs.kotlinx.coroutines.swing
            )
        }

        wasmJsMain.configure {
            useSourceSet(skikoMain)
            useApi(
                libs.kotlinx.broswer
            )
        }
    }
}