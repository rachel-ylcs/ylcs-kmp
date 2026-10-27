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
        windowsTarget = true
        linuxTarget = true
        macosTarget = true

        sources {
            commonMain {
                lib(
                    ExportLib,
                    libs.androidx.savedstate,
                    libs.androidx.viewmodel,
                    libs.androidx.lifecycle,
                    libs.compose.runtime,
                    libs.compose.ui,
                    libs.compose.foundation,

                    projects.ylcsModule.foundation.context,
                )
            }

            appleMain(commonMain)

            skikoMain(commonMain) {
                lib(
                    ExportLib,
                    libs.skiko
                )
            }

            iosMain(skikoMain, appleMain)

            iosMainList(iosMain)

            desktopMain(skikoMain)

            windowsMain(skikoMain)

            linuxMain(skikoMain)

            macosMain(skikoMain)

            webMain(skikoMain)

            jsMain(webMain)

            wasmJsMain(webMain)
        }
    }
}
