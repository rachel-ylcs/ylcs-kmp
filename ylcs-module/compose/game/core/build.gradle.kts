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
                    projects.ylcsModule.compose.core
                )
            }

            skikoMain(commonMain)

            iosMainList(skikoMain)

            desktopMain(skikoMain)

            webMain(skikoMain)

            jsMain(webMain)

            wasmJsMain(webMain)
        }
    }
}
