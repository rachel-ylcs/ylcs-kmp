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
                    projects.ylcsModule.foundation.network,
                    libs.sketch,
                    libs.sketch.http,
                    libs.sketch.resources,
                    libs.sketch.gif,
                    libs.sketch.webp,
                    libs.sketch.extensions.compose,
                    libs.sketch.zoom,
                    libs.compose.resources,
                    ExportLib,
                    projects.ylcsModule.foundation.startup,
                    projects.ylcsModule.compose.ui,
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
