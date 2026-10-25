plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    `maven-publish`
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.annotation)
            api(project(":ui-geometry"))
            implementation(libs.androidx.collection)
            implementation(libs.compose.runtime.asProvider())
            implementation(project(":ui-util"))
        }
        getByName("nativeMain").dependencies {
            implementation(libs.kotlinx.atomicfu)
            implementation(libs.compose.annotation.internal)
            implementation(libs.compose.collection.internal)
        }
    }
}
