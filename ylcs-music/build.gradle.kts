import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
}

kotlin {
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
        commonMain.dependencies {
            implementation(projects.ylcsShared)

            implementation(libs.compose.runtime)
            implementation(libs.kotlinx.json)
            implementation(libs.kotlinx.io)
        }
    }
}

android {
    namespace = "${rootProject.extra["appPackageName"]}.music"
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