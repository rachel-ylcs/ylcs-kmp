import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidLibrary1)
}

kotlin {
    C.useCompilerFeatures(this)

    android {
        namespace = "${C.app.packageName}.module.compose.ui"
        compileSdk = C.android.compileSdk
        minSdk = C.android.minSdk
        lint.targetSdk = C.android.targetSdk

        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(C.jvm.target)
                }
            }
        }

        androidResources.enable = true
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
                projects.ylcsModule.compose.context,
                projects.ylcsModule.compose.device,
                projects.ylcsModule.compose.theme,
                libs.compose.material3,
                libs.compose.material3.icons,
                libs.compose.material3.iconsExtended,
                libs.compose.ui,
            )
            useLib(
                libs.compose.components.resources,
                libs.compose.navigation.event,
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
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "${C.app.packageName}.compose.ui.resources"
}