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

        interop {
            desktop {
                cpp("src/nativeMain/cpp")
            }
            native {
                cpp("audio")
            }
        }

        sources {
            commonMain {
                lib(
                    libs.compose.resources,
                    ExportLib,
                    projects.ylcsModule.foundation.filesystem,
                    projects.ylcsModule.compose.ui,
                    projects.ylcsModule.compose.platformView,
                )
            }

            androidMain(commonMain) {
                lib(
                    ExportLib,
                    libs.media3.ui,
                    libs.media3.player,
                    libs.media3.session,
                )
            }

            iosMain(commonMain)

            iosMainList(iosMain)

            pcMain(commonMain)

            desktopMain(pcMain) {
                lib(projects.ylcsModule.foundation.libloader)
            }

            nativeMain(pcMain)

            windowsMain(nativeMain)

            linuxMain(nativeMain)

            macosMain(nativeMain)

            webMain(commonMain) {
                lib(projects.ylcsModule.compose.platformView)
            }

            jsMain(webMain)

            wasmJsMain(webMain)
        }
    }
}
