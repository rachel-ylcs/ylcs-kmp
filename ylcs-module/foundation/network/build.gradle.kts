import love.yinlin.gradle.ExportLib

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
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
                    libs.ktor.json,
                    libs.ktor.client.negotiation,
                    libs.ktor.client.encoding,
                    libs.ktor.client.websockets,

                    projects.ylcsModule.foundation.compression,
                    ExportLib,
                    libs.ktor.client,
                    projects.ylcsModule.foundation.filesystem,
                )
            }

            androidMain(commonMain) {
                lib(libs.ktor.okhttp)
            }

            appleMain(commonMain) {
                lib(libs.ktor.apple)
            }

            iosMain(appleMain)

            iosMainList(iosMain)

            desktopMain(commonMain) {
                lib(libs.ktor.okhttp)
            }

            nativeMain(commonMain)

            windowsMain(nativeMain) {
                lib(libs.ktor.windows)
            }

            linuxMain(nativeMain) {
                lib(libs.ktor.cio)
            }

            macosMain(appleMain)

            webMain(commonMain) {
                lib(libs.ktor.js)
            }

            jsMain(webMain)

            wasmJsMain(webMain)
        }
    }
}
