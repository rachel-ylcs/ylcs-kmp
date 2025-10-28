import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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
                projects.ylcsBase.core,
                libs.compose.runtime,
                libs.compose.foundation,
                libs.compose.savedstate,
                libs.compose.viewmodel,
                libs.compose.lifecycle,
                libs.compose.components.uiToolingPreview,
            )
        }

        androidMain.configure {
            useApi(
                compose.preview,
                libs.compose.activity,
            )
        }

        val skikoMain by creating {
            useSourceSet(commonMain)
            useApi(
                libs.skiko,
            )
        }

        val iosMain = iosMain.get().apply {
            useSourceSet(skikoMain)
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
            useSourceSet(skikoMain)
            useApi(
                compose.desktop.currentOs,
            )
        }

        wasmJsMain.configure {
            useSourceSet(skikoMain)
        }
    }
}

dependencies {
    implementation(libs.compose.ui.graphics.android)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.android)
}

android {
    namespace = "${C.app.packageName}.base.compose_core"
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