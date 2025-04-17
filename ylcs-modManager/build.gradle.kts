import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    
    sourceSets {
        jvmMain.dependencies {
            implementation(projects.ylcsShared)
            implementation(projects.ylcsMusic)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material3.icons)
            implementation(libs.compose.ui)

            implementation(libs.kotlinx.coroutines)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.kotlinx.io)
            implementation(libs.kotlinx.json)

            implementation(libs.runtime.viewmodel)
            implementation(libs.runtime.lifecycle)

            implementation(compose.desktop.currentOs)
        }
    }
}


compose.desktop {
    application {
        mainClass = "love.yinlin.mod.ModManagerKt"

        buildTypes.release.proguard {
            isEnabled = true
            optimize = true
            obfuscate = true
            configurationFiles.from(
                rootProject.extra["commonR8File"],
                rootProject.extra["desktopR8File"]
            )
        }

        nativeDistributions {
            packageName = "love.yinlin.mod.ModManager"

            targetFormats(TargetFormat.Exe)
        }
    }
}
