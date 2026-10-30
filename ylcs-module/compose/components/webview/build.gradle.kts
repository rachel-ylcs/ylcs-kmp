import love.yinlin.gradle.ExportLib

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.nativeBridge,
        libs.plugins.androidLibrary,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

rachel {
    multiplatform {
        nativeDesktopTarget = true

        sources {
            commonMain {
                lib(
                    projects.ylcsModule.compose.ui,
                    ExportLib,
                    projects.ylcsModule.compose.platformView,
                )
            }

            iosMain(commonMain)

            iosMainList(iosMain)

            nativeMain(commonMain)

            windowsMain(nativeMain)

            linuxMain(nativeMain)

            macosMain(nativeMain)

            webMain(commonMain)

            jsMain(webMain)

            wasmJsMain(webMain)
        }
    }
}
