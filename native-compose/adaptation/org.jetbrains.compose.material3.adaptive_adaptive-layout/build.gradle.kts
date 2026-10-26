plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("love.yinlin.native-bridge")
    `maven-publish`
}

nativeBridge { skikoTests() }

kotlin {
    compilerOptions.optIn.addAll("androidx.compose.material3.ExperimentalMaterial3Api", "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi", "androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi", "androidx.compose.foundation.ExperimentalFoundationApi", "androidx.compose.animation.ExperimentalAnimationApi", "kotlin.experimental.ExperimentalNativeApi", "kotlinx.cinterop.ExperimentalForeignApi", "kotlin.time.ExperimentalTime")
    sourceSets {
        commonMain.dependencies {
            api(project(":adaptive"))
            api(project(":animation"))
            api(project(":animation-core"))
            api(project(":foundation"))
            api(project(":foundation-layout"))
            api(project(":ui"))
            api(project(":ui-geometry"))
            api(libs.androidx.window.core)
            implementation(libs.androidx.collection)
        }
    }
}
