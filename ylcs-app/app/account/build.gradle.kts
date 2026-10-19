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
                    projects.ylcsApp.app.activity,
                    projects.ylcsApp.app.community,
                    projects.ylcsApp.app.viewer,

                    projects.ylcsModule.compose.components.paginationLayout,
                    projects.ylcsModule.compose.components.qrcode,
                )
            }
        }
    }
}
