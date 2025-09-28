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
                projects.ylcsComposeCore,
                projects.ylcsMod,
                // compose
                libs.compose.components.resources,
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