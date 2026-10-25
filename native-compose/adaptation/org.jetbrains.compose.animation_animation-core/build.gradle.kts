plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("love.yinlin.native-bridge")
    `maven-publish`
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(project(":ui"))
            implementation(project(":ui-unit"))
            implementation(project(":ui-graphics"))
            implementation(project(":ui-util"))
            implementation(libs.androidx.collection)
            implementation(libs.kotlinx.atomicfu)
            api(libs.kotlinx.coroutines)
        }
    }
}
