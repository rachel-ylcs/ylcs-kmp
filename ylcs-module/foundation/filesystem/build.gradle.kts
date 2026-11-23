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
                windows {
                    cpp("win32")
                }
            }
        }

        sources {
            commonMain {
                lib(
                    ExportLib,
                    projects.ylcsModule.foundation.context
                )
            }

            commonTest {
                lib(
                    libs.test,
                    libs.kotlinx.coroutines.test
                )
            }

            clientMain(commonMain)

            jvmMain(clientMain)

            nativeMain(clientMain)

            appleMain(nativeMain)

            androidMain(jvmMain)

            iosMain(appleMain)

            iosMainList(iosMain)

            desktopMain(jvmMain)

            webMain(commonMain)

            jsMain(webMain)

            wasmJsMain(webMain)

            webTest(commonTest)

            jsTest(webTest)

            wasmJsTest(webTest)

            windowsMain(nativeMain)

            linuxMain(nativeMain)

            macosMain(appleMain)
        }
    }
}
