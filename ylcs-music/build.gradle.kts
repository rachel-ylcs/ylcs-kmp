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
    }

    iosArm64()
    if (C.platform == BuildPlatform.Mac) {
        when (C.architecture) {
            BuildArchitecture.AARCH64 -> iosSimulatorArm64()
            BuildArchitecture.X86_64 -> iosX64()
            else -> {}
        }
    }

    jvm {
        C.jvmTarget(this)
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.configure {
            useLib(
                // project
                projects.ylcsShared,
                // compose
                libs.compose.runtime,
                // kotlinx
                libs.kotlinx.json,
                libs.kotlinx.io
            )
        }
    }
}

android {
    namespace = "${C.app.packageName}.music"
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