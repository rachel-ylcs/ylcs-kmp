plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.nativeBridge,
        libs.plugins.androidLibrary,
    )
}

rachel {
    multiplatform {
        nativeDesktopTarget = true

        sources {
            commonMain {
                lib(
                    projects.ylcsApp.app.global,
                    projects.ylcsApp.app.gameGuessLyrics,
                    projects.ylcsApp.app.gameRhyme,

                    projects.ylcsModule.compose.components.paginationLayout
                )
            }
        }
    }
}
