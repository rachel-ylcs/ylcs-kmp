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
                    ExportLib,
                    projects.ylcsModule.compose.core,
                )
            }

            androidMain(commonMain) {
                lib(libs.lottie.android)
            }

            skikoMain(commonMain) {
                lib(libs.skiko.lottie)
            }

            iosMainList(skikoMain)

            desktopMain(skikoMain) {
                lib(libs.skiko.lottie.jvm)
            }

            nativeMain(skikoMain)

            windowsMain(nativeMain)

            linuxMain(nativeMain)

            macosMain(nativeMain)

            webMain(skikoMain)

            jsMain(webMain)

            wasmJsMain(webMain)
        }
    }
}
