plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("love.yinlin.native-bridge")
    `maven-publish`
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.navigation3.runtime)
            api(project(":animation"))
            api(project(":ui"))
            api(libs.androidx.navigation.event)
            api(libs.androidx.lifecycle)
            api(libs.androidx.savedstate.compose)
            implementation(libs.androidx.collection)
            implementation(libs.androidx.annotation)
        }
    }
}
