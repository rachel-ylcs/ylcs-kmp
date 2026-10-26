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
            api(project(":foundation-layout"))
            api(project(":material-ripple"))
            api(libs.compose.runtime)
            api(project(":ui"))
            api(project(":foundation"))
            api(project(":ui-text"))
            api(libs.androidx.graphics.shapes)
            implementation(project(":animation-core"))
            implementation(project(":ui-util"))
            implementation(project(":material3-ripple"))
            implementation(libs.androidx.collection)
            implementation(libs.kotlinx.datetime)
        }
        getByName("nativeMain").dependencies {
            implementation(libs.kotlinx.atomicfu)
            implementation(libs.native.skiko)
        }
    }
}
