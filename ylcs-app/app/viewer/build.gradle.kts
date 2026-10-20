import love.yinlin.gradle.ExportLib

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
                    projects.ylcsApp.app.global,
                    ExportLib,
                    projects.ylcsModule.compose.components.webview,
                    projects.ylcsModule.compose.components.media
                )
            }
        }
    }
}
