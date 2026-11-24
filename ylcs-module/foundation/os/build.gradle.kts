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

        interop {
            native {
                macos {
                    cpp("single_instance")
                    cpp("battery_level")
                }
            }
        }

        sources {
            commonMain {
                lib(
                    ExportLib,
                    projects.ylcsModule.foundation.context,
                    libs.compose.runtime,
                )
            }

            commonTest {
                lib(libs.test)
            }

            pcMain(commonMain)

            androidMain(commonMain)

            iosMain(commonMain)

            iosMainList(iosMain)

            desktopMain(pcMain) {
                lib(
                    projects.ylcsModule.platform.ffi.posix,
                    projects.ylcsModule.platform.ffi.win32,
                )
            }

            nativeMain(pcMain)

            windowsMain(nativeMain)

            linuxMain(nativeMain)

            macosMain(nativeMain)

            webMain(commonMain)

            jsMain(webMain)

            wasmJsMain(webMain)
        }
    }
}
