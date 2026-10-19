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
                    ExportLib,
                    projects.ylcsModule.core,
                )
            }

            androidMain(commonMain) {
                lib(
                    ExportLib,
                    libs.androidx.activity,
                )
            }

            val skikoMain = createSkiko(commonMain)

            iosMain(skikoMain)

            iosMainList(iosMain)

            desktopMain(skikoMain)

            webMain(skikoMain)

            jsMain(webMain)

            wasmJsMain(webMain)

            nativeMain(commonMain)

            windowsMain(nativeMain)

            linuxMain(nativeMain)

            macosMain(nativeMain)
        }
    }
}
