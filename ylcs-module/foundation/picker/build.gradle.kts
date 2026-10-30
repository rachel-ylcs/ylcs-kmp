import love.yinlin.gradle.ExportLib

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
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
            val sdlSource = rootProject.file("native-compose/vendor/sdl-source-${libs.versions.sdl.get()}")

            desktop {
                cpp(sdlSource.path)
            }

            native {
                cpp("sdl3") {
                    dependencyFiles = files()
                    includeDirs(sdlSource.resolve("include").path)
                }
            }
        }

        sources {
            commonMain {
                lib(
                    ExportLib,
                    projects.ylcsModule.foundation.filesystem,
                )
            }

            iosMain(commonMain)

            iosMainList(iosMain)

            desktopMain(commonMain) {
                lib(projects.ylcsModule.platform.nativeLibLoader)
            }

            webMain(commonMain)

            jsMain(webMain)

            wasmJsMain(webMain)

            nativeMain(commonMain) {
                lib(libs.skiko)
            }

            windowsMain(nativeMain)

            linuxMain(nativeMain)

            macosMain(nativeMain)
        }
    }
}
