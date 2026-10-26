plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("love.yinlin.native-bridge")
    `maven-publish`
}

kotlin {
    compilerOptions.optIn.addAll("org.jetbrains.compose.resources.InternalResourceApi", "org.jetbrains.compose.resources.ExperimentalResourceApi", "kotlinx.cinterop.ExperimentalForeignApi", "kotlin.experimental.ExperimentalNativeApi")
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(project(":foundation"))
            implementation(libs.kotlinx.coroutines)
        }
        getByName("skikoMain").dependencies {
            implementation(libs.native.skiko)
        }
        getByName("nativeMain").dependencies {
            implementation(libs.kotlinx.atomicfu)
        }
        // SDL's CInterop types are target-specific, so only the leaf fragments compile this backend.
        val platformSources = rootProject.layout.projectDirectory.dir("../../../../../native-compose/adaptation/org.jetbrains.compose.components_components-resources/src/nativePlatformMain/kotlin")
        for (target in listOf("windows", "linux", "macos")) getByName("${target}Main").kotlin.srcDir(platformSources)
    }
}
