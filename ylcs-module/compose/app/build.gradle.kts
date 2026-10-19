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
                    projects.ylcsModule.platform.nativeLibLoader,
                    libs.compose.resources,
                    ExportLib,
                    projects.ylcsModule.foundation.startup,
                    projects.ylcsModule.foundation.filesystem,
                    projects.ylcsModule.compose.ui,
                )
            }

            androidMain(commonMain) {
                lib(
                    ExportLib,
                    libs.androidx.activity.compose
                )
            }

            iosMain(commonMain)

            iosMainList(iosMain)

            desktopMain(commonMain)

            webMain(commonMain)

            jsMain(webMain)

            wasmJsMain(webMain)
        }
    }
}
