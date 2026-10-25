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
            api(project(":ui-geometry"))
            api(project(":ui-graphics"))
            api(project(":ui-text"))
            api(project(":ui-unit"))
            api(project(":ui-util"))
            implementation(libs.compose.runtime.asProvider())
            api(libs.compose.runtime.retain)
            api(libs.compose.runtime.saveable)
            api(libs.native.skiko.asProvider())
            api(libs.androidx.lifecycle)
            api(libs.androidx.savedstate.compose)
            implementation(libs.androidx.collection)
            implementation(libs.kotlinx.coroutines.asProvider())
        }
        getByName("skikoMain").dependencies {
            api(project(":ui-skiko"))
            implementation(project(":ui-backhandler"))
            implementation(libs.kotlinx.atomicfu)
            implementation(libs.androidx.navigation.event)
            implementation(libs.androidx.viewmodel)
            implementation(libs.androidx.viewmodel.savedstate)
        }
    }
}

nativeBridge { skikoTests() }
