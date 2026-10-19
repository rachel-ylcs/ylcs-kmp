import love.yinlin.gradle.*

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.androidLibrary,
    )
}

rachel {
    multiplatform {
        sources {
            commonMain {
                lib(
                    libs.compose.resources,

                    projects.ylcsApp.app.global,
                    projects.ylcsApp.app.viewer,

                    projects.ylcsModule.foundation.parser.lrc,

                    projects.ylcsModule.compose.components.blur,
                    projects.ylcsModule.compose.components.dragDrop,
                    projects.ylcsModule.compose.components.paginationLayout,
                    projects.ylcsModule.compose.components.media,
                    projects.ylcsModule.compose.components.reordering,
                )
            }

            commonTest {
                lib(libs.test)
            }

            iosMain(commonMain)

            iosMainList(iosMain)

            desktopMain(commonMain) {
                lib(projects.ylcsModule.platform.os.window)
            }

            webMain(commonMain)

            jsMain(webMain)

            wasmJsMain(webMain)
        }
    }
}
