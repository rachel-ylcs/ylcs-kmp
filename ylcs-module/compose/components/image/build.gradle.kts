import love.yinlin.gradle.ExportLib

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.nativeBridge,
        libs.plugins.androidLibrary,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

rachel {
    multiplatform {
        nativeDesktopTarget = true

        sources {
            commonMain {
                lib(
                    projects.ylcsModule.compose.cache,
                    ExportLib,
                    libs.compose.resources,
                    projects.ylcsModule.foundation.network,
                    projects.ylcsModule.compose.ui,
                )
            }

            jvmMain(commonMain)

            androidMain(jvmMain)

            skikoMain(commonMain)

            iosMain(skikoMain)

            iosMainList(iosMain)

            desktopMain(skikoMain, jvmMain)

            nativeMain(skikoMain)

            windowsMain(nativeMain)

            linuxMain(nativeMain)

            macosMain(nativeMain)

            webMain(skikoMain)

            jsMain(webMain)

            wasmJsMain(webMain)
        }
    }
}
