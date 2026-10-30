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
            ios {
                discoverClangModulesImplicitly = false
                spm("https://github.com/Tencent/MMKV.git", libs.versions.mmkv.get(), "MMKV")
                spm("Dummy")
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
                lib(libs.mmkv.android)
            }

            iosMain(commonMain)

            iosMainList(iosMain)

            desktopMain(commonMain) {
                lib(projects.ylcsModule.platform.nativeLibLoader)
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
