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
                    libs.compose.resources,
                    projects.ylcsApp.app.global,

                    projects.ylcsModule.compose.components.media,
                )
            }
        }
    }
}
