import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
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
            implementation(libs.kotlinx.coroutines)
            implementation(libs.kotlinx.json)
            implementation(libs.korge)
        }
    }
}

configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "net.java.dev.jna" && requested.name == "jna-jpms") useTarget(libs.jna)
            else if (requested.group == "net.java.dev.jna" && requested.name == "jna-platform-jpms") useTarget(libs.jna.platform)
        }
        force(libs.jna)
        force(libs.jna.platform)
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