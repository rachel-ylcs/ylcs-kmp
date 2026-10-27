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
                    ExportLib,
                    libs.androidx.graphics.shapes,
                    libs.androidx.window.core,
                    libs.androidx.savedstate,
                    libs.androidx.savedstate.compose,
                    libs.androidx.viewmodel,
                    libs.androidx.lifecycle,
                    libs.androidx.viewmodel.savedstate,
                    libs.compose.runtime,
                    libs.compose.runtime.saveable,
                    libs.compose.runtime.retain,
                    libs.compose.ui,
                    libs.compose.ui.util,
                    libs.compose.ui.geometry,
                    libs.compose.ui.unit,
                    libs.compose.ui.graphics,
                    libs.compose.ui.text,
                    libs.compose.ui.skiko,
                    libs.compose.ui.backhandler,
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
