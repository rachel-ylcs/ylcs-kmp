plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("love.yinlin.native-bridge")
    `maven-publish`
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":animation"))
            api(project(":foundation-layout"))
            api(libs.compose.runtime)
            api(project(":ui"))
            api(project(":ui-geometry"))
            implementation(project(":ui-util"))
            implementation(libs.androidx.collection)
        }
    }
}
