plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("love.yinlin.native-bridge")
    `maven-publish`
}

kotlin {
    compilerOptions.optIn.addAll("androidx.compose.ui.InternalComposeUiApi", "androidx.compose.ui.graphics.ExperimentalGraphicsApi")
    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.annotation)
            api(project(":ui-unit"))
            implementation(libs.compose.runtime)
            implementation(project(":ui-util"))
            implementation(libs.androidx.collection)
        }
        getByName("nativeMain").dependencies {
            api(libs.skiko)
            implementation(libs.kotlinx.atomicfu)
            implementation(libs.compose.collection.internal)
        }
    }
}
