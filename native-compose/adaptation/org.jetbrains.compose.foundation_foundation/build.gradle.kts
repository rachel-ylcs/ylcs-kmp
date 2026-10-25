plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("love.yinlin.native-bridge")
    `maven-publish`
}

kotlin {
    compilerOptions.optIn.addAll("androidx.compose.foundation.ExperimentalFoundationApi", "androidx.compose.ui.InternalComposeUiApi", "androidx.compose.ui.text.InternalTextApi")
    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.collection)
            api(project(":animation"))
            api(libs.compose.runtime)
            api(project(":ui"))
            implementation(project(":ui-text"))
            implementation(project(":ui-util"))
            implementation(project(":foundation-layout"))
            implementation(libs.androidx.graphics.shapes)
        }
        getByName("skikoMain").dependencies {
            api(project(":ui-skiko"))
            implementation(libs.native.skiko)
            implementation(libs.kotlinx.atomicfu)
        }
    }
}
