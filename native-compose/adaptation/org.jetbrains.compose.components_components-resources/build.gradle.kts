plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("love.yinlin.native-bridge")
    `maven-publish`
}

nativeBridge { skikoTests() }

kotlin {
    compilerOptions.optIn.addAll("org.jetbrains.compose.resources.InternalResourceApi", "org.jetbrains.compose.resources.ExperimentalResourceApi", "kotlinx.cinterop.ExperimentalForeignApi", "kotlin.experimental.ExperimentalNativeApi")
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(project(":foundation"))
            implementation(libs.kotlinx.coroutines)
        }
        getByName("skikoMain").dependencies {
            implementation(libs.native.skiko)
        }
        getByName("nativeMain").dependencies {
            implementation(libs.kotlinx.atomicfu)
        }
    }
}
