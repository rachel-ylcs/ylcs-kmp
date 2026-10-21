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
        windowsTarget = true
        linuxTarget = true
        macosTarget = true

        webTestHeadless = false

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

            windowsMain(nativeMain)

            linuxMain(nativeMain)

            macosMain(appleMain)
        }

        windows {
            compilations.getByName("main") {
                cinterops.create("win32")
            }
        }
    }
}
