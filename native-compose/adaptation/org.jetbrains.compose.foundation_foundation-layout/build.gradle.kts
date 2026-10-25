plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("love.yinlin.native-bridge")
    `maven-publish`
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":ui"))
            implementation(libs.compose.runtime)
            implementation(project(":ui-util"))
            implementation(project(":ui-unit"))
            implementation(libs.androidx.collection)
        }
    }
}
