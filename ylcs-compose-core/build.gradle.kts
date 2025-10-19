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
                projects.ylcsCore,
                libs.compose.runtime,
                libs.compose.foundation,
                libs.compose.material3,
                libs.compose.material3.icons,
                libs.compose.material3.iconsExtended,
                libs.compose.ui,
                libs.compose.ui.backhandler,
                libs.compose.components.resources,
                libs.compose.components.uiToolingPreview,
                libs.compose.navigation,
                libs.compose.navigation.event,
                libs.compose.savedstate,
                libs.compose.viewmodel,
                libs.compose.lifecycle,
            )
        }

        val nonAndroidMain by creating {
            useSourceSet(commonMain)
        }

        val iosMain = iosMain.get().apply {
            useSourceSet(nonAndroidMain)
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
            useSourceSet(nonAndroidMain)
        }

        wasmJsMain.configure {
            useSourceSet(nonAndroidMain)
        }
    }
}

android {
    namespace = "${C.app.packageName}.compose.core"
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