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
            api(project(":ui-unit"))
            implementation(libs.compose.runtime.asProvider())
            implementation(libs.compose.runtime.saveable)
            implementation(project(":ui-util"))
            implementation(libs.kotlinx.coroutines.asProvider())
            implementation(libs.androidx.collection)
        }
        getByName("nativeMain") {
            dependencies {
                implementation(libs.native.skiko.asProvider())
                implementation(libs.kotlinx.atomicfu)
                implementation(libs.compose.collection.internal)
            }
        }
    }
}

nativeBridge { skikoTests() }
