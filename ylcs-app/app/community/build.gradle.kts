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
                    projects.ylcsApp.app.viewer,
                    projects.ylcsModule.compose.components.paginationLayout,
                )
            }
        }
    }
}
