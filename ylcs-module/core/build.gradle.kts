import love.yinlin.gradle.*

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

            val clientMain = createClient(commonMain)

            nativeMain(clientMain)

            val jvmMain = createJvm(clientMain) {
                lib(
                    ExportLib,
                    libs.androidx.collection,
                )
            }

            appleMain(nativeMain) {
                lib(
                    ExportLib,
                    libs.androidx.collection,
                )
            }

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

            webMain(commonMain) {
                lib(
                    ExportLib,
                    libs.androidx.collection,
                    libs.kotlinx.broswer
                )
            }

            jsMain(webMain)

            wasmJsMain(webMain)

            windowsMain(nativeMain) {
                lib(
                    ExportLib,
                    libs.androidx.collection,
                )
            }

            linuxMain(nativeMain) {
                lib(
                    ExportLib,
                    libs.androidx.collection,
                )
            }

            macosMain(appleMain)
        }
    }
}
