plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("love.yinlin.native-bridge")
    `maven-publish`
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime.asProvider())
            implementation(project(":ui-util"))
            implementation(libs.androidx.annotation)
            implementation(libs.kotlinx.coroutines.asProvider())
        }
        getByName("nativeMain").dependencies {
            implementation(libs.androidx.navigation.event)
        }
    }
}
