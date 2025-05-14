import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.compose.runtime)

                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.json)
            }
        }

        val nonAndroidMain by creating {
            dependsOn(commonMain)
            dependencies {

            }
        }

        val nonWasmJsMain by creating {
            dependsOn(commonMain)
            dependencies {

            }
        }

        androidMain.get().apply {
            dependsOn(nonWasmJsMain)
            dependencies {

            }
        }

        val iosMain = iosMain.get().apply {
            dependsOn(nonAndroidMain)
            dependsOn(nonWasmJsMain)
            dependencies {

            }
        }

        listOf(
            iosX64Main,
            iosArm64Main,
            iosSimulatorArm64Main
        ).forEach {
            it.get().apply {
                dependsOn(iosMain)
                dependencies {

                }
            }
        }

        jvmMain.get().apply {
            dependsOn(nonWasmJsMain)
            dependencies {

            }
        }

        wasmJsMain.get().apply {
            dependsOn(nonAndroidMain)
            dependencies {

            }
        }
    }
}

android {
    namespace = "${rootProject.extra["appPackageName"]}.shared"
    compileSdk = rootProject.extra["androidBuildSDK"] as Int

    defaultConfig {
        minSdk = rootProject.extra["androidMinSDK"] as Int
        lint.targetSdk = rootProject.extra["androidBuildSDK"] as Int
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}