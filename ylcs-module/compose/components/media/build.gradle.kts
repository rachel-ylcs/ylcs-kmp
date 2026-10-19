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
                    libs.compose.resources,
                    ExportLib,
                    projects.ylcsModule.foundation.filesystem,
                    projects.ylcsModule.compose.ui,
                    projects.ylcsModule.compose.platformView,
                )
            }

            androidMain(commonMain) {
                lib(
                    ExportLib,
                    libs.media3.ui,
                    libs.media3.player,
                    libs.media3.session,
                )
            }

            iosMain(commonMain)

            iosMainList(iosMain)

            desktopMain(commonMain) {
                lib(projects.ylcsModule.platform.nativeLibLoader)
            }

            webMain(commonMain) {
                lib(projects.ylcsModule.compose.platformView)
            }

            jsMain(webMain)

            wasmJsMain(webMain)
        }
    }
}
