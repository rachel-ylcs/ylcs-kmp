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
                    cpp("iokit")
                }
            }
        }

        android {
            withDeviceTest {
                instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
        }

        sources {
            commonMain {
                lib(
                    ExportLib,
                    projects.ylcsModule.foundation.context,
                )
            }

            commonTest {
                lib(libs.test, libs.kotlinx.coroutines.test)
            }

            pcMain(commonMain)

            desktopTest(commonTest)

            androidMain(commonMain)

            iosMain(commonMain)

            iosMainList(iosMain)
            iosTest(commonTest)
            iosTestList(iosTest)

            desktopMain(pcMain) {
                lib(
                    projects.ylcsModule.platform.ffi.posix,
                    projects.ylcsModule.platform.ffi.win32,
                    projects.ylcsModule.platform.ffi.macos,
                )
            }

            nativeMain(pcMain)

            windowsMain(nativeMain)
            windowsTest(commonTest)

            linuxMain(nativeMain)
            linuxTest(commonTest)

            macosMain(nativeMain)
            macosTest(commonTest)

            webMain(commonMain)

            jsMain(webMain)
            jsTest(commonTest)

            wasmJsMain(webMain)
            wasmJsTest(commonTest)

            find("androidDeviceTest", commonTest) {
                lib(libs.androidx.test.core, libs.androidx.test.runner)
            }
        }
    }
}
