import love.yinlin.gradle.*

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.androidLibrary,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

rachel {
    multiplatform {
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

            val skikoMain = createSkiko(commonMain) {
                lib(
                    ExportLib,
                    libs.skiko
                )
            }

            iosMain(skikoMain, appleMain)

            iosMainList(iosMain)

            desktopMain(skikoMain)

            webMain(skikoMain)

            jsMain(webMain)

            wasmJsMain(webMain)
        }
    }
}
