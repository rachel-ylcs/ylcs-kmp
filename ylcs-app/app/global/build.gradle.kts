import love.yinlin.gradle.ExportLib

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

                    ExportLib,

                    projects.ylcsApp.cs,
                    projects.ylcsApp.mod,
                    projects.ylcsApp.thirdParty,

                    projects.ylcsModule.foundation.cryptography,
                    projects.ylcsModule.foundation.picker,

                    projects.ylcsModule.cs.clientEngine,

                    projects.ylcsModule.foundation.exception,
                    projects.ylcsModule.compose.config,

                    projects.ylcsModule.compose.app,
                    projects.ylcsModule.compose.cache,
                    projects.ylcsModule.compose.icons,
                    projects.ylcsModule.compose.screen,

                    projects.ylcsModule.compose.components.image,
                    projects.ylcsModule.compose.components.lottie,
                    projects.ylcsModule.compose.components.richText,
                )
            }
        }
    }
}
