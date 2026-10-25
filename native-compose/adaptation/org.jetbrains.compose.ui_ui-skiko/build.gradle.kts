plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("love.yinlin.native-bridge")
    `maven-publish`
}

kotlin {
    compilerOptions.optIn.addAll("androidx.compose.ui.InternalComposeUiApi", "androidx.compose.ui.text.ExperimentalTextApi")
    sourceSets {
        commonMain.dependencies {
            api(project(":ui-graphics"))
            api(project(":ui-text"))
            api(libs.native.skiko.asProvider())
            implementation(libs.compose.runtime.asProvider())
            implementation(project(":ui-util"))
        }
        getByName("nativeMain").dependencies {
            implementation(libs.kotlinx.atomicfu)
        }
    }
}

nativeBridge { skikoTests() }
