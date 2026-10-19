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
                    projects.ylcsModule.foundation.startup,
                )
            }

            appleMain(commonMain)

            val jvmMain = createJvm(commonMain)

            androidMain(jvmMain)

            iosMain(appleMain)

            iosMainList(iosMain)

            desktopMain(jvmMain)

            webMain(commonMain)

            jsMain(webMain)

            wasmJsMain(webMain)

            nativeMain(commonMain)

            windowsMain(nativeMain)

            linuxMain(nativeMain)

            macosMain(appleMain)
        }
    }
}
