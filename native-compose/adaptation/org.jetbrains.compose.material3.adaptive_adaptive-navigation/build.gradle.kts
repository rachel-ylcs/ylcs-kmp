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
            api(project(":adaptive-layout"))
            api(project(":foundation"))
            api(project(":ui-util"))
        }
    }
}
