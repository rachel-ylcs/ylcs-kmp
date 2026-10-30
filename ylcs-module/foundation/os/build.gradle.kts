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
                }
            }
        }

        sources {
            androidMain(commonMain)

            iosMain(commonMain)

            iosMainList(iosMain)

            desktopMain(commonMain) {
                lib(
                    projects.ylcsModule.platform.ffi.posix,
                    projects.ylcsModule.platform.ffi.win32,
                )
            }

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
