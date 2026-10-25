plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    `maven-publish`
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(project(":ui-util"))
        }
    }
}
