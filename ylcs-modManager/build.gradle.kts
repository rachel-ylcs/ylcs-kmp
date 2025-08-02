import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    C.useCompilerFeatures(this)

    jvm {
        C.jvmTarget(this)
    }
    
    sourceSets {
        jvmMain.configure {
            useLib(
                // project
                projects.ylcsShared, projects.ylcsMusic,
                // compose
                libs.compose.runtime, libs.compose.foundation,
                libs.compose.material3, libs.compose.material3.icons,
                libs.compose.ui,
                compose.desktop.currentOs,
                // kotlinx
                libs.kotlinx.coroutines, libs.kotlinx.coroutines.swing,
                libs.kotlinx.io, libs.kotlinx.json, libs.kotlinx.datetime,
                // runtime
                libs.runtime.viewmodel, libs.runtime.lifecycle,
            )
        }
    }
}

compose.desktop {
    application {
        mainClass = C.modManager.mainClass

        buildTypes.release.proguard {
            version = C.proguard.version
            isEnabled = true
            optimize = true
            obfuscate = true
            configurationFiles.from(C.root.app.commonR8Rule, C.root.app.desktopR8Rule)
        }

        nativeDistributions {
            packageName = C.modManager.name

            targetFormats(TargetFormat.Exe)

            windows {
                console = false
            }
        }
    }
}
