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

            webMain(commonMain) {
                lib(libs.ktor.js)
            }

            jsMain(webMain)

            wasmJsMain(webMain)

            windowsMain(commonMain) {
                lib(libs.ktor.windows)
            }

            linuxMain(commonMain) {
                lib(libs.ktor.cio)
            }

            macosMain(appleMain)
        }
    }
}
