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

        sources {
            commonMain {
                lib(
                    libs.kotlinx.atomicfu,
                    ExportLib,
                    libs.androidx.annotation,
                    libs.androidx.collection,

                    libs.kotlinx.coroutines,
                    libs.kotlinx.io,
                    libs.kotlinx.datetime,
                    libs.kotlinx.json,
                )
            }

            commonTest {
                lib(
                    libs.test,
                    libs.kotlinx.coroutines.test
                )
            }

            clientMain(commonMain)

            nativeMain(clientMain)

            jvmMain(clientMain)

            appleMain(nativeMain)

            androidMain(jvmMain) {
                lib(
                    ExportLib,
                    libs.kotlinx.coroutines.android,
                )
            }

            iosMain(appleMain)

            iosMainList(iosMain)

            // appleMain是原生objc，desktopMain是基于jvm的，不许在这加appleMain了:(
            desktopMain(jvmMain) {
                lib(
                    ExportLib,
                    libs.kotlinx.coroutines.swing
                )
            }

            windowsMain(nativeMain)

            linuxMain(nativeMain)

            macosMain(appleMain)

            webMain(commonMain) {
                lib(
                    ExportLib,
                    libs.kotlinx.broswer
                )
            }

            jsMain(webMain)

            wasmJsMain(webMain)
        }
    }
}
