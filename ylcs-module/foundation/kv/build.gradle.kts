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
            desktop {
                cpp("src/nativeMain/cpp")
            }
            native {
                cpp("kv")
            }
        }

        sources {
            commonMain {
                lib(
                    ExportLib,
                    projects.ylcsModule.foundation.filesystem,
                )
            }

            androidMain(commonMain) {
                lib(
                    ExportLib,
                    libs.mmkv
                )
            }

            iosMain(commonMain) {
                lib(
                    ExportLib,
                    libs.mmkv
                )
            }

            iosMainList(iosMain)

            desktopMain(commonMain) {
                lib(projects.ylcsModule.foundation.libloader)
            }

            webMain(commonMain)

            jsMain(webMain)

            wasmJsMain(webMain)

            nativeMain(commonMain)

            windowsMain(nativeMain)

            linuxMain(nativeMain)

            macosMain(nativeMain)
        }
    }
}
