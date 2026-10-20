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
                    libs.compose.resources,
                    projects.ylcsApp.app.global,
                    projects.ylcsApp.app.music,

                    projects.ylcsModule.compose.components.media,
                    projects.ylcsModule.compose.game.core,

                    ExportLib,
                    projects.ylcsModule.compose.components.blur,
                )
            }
        }
    }
}
