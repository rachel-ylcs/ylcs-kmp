plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.androidLibraryNew,
        libs.plugins.kotlinSerialization,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                libs.compose.resources,
                libs.lottie,
                libs.lottie.network,

                ExportLib,

                projects.ylcsApp.cs,

                projects.ylcsModule.cs.clientEngine,

                projects.ylcsModule.startup.os,
                projects.ylcsModule.startup.picker,
                projects.ylcsModule.startup.exception,
                projects.ylcsModule.compose.startup.config,

                projects.ylcsModule.compose.app,
                projects.ylcsModule.compose.screen,
                projects.ylcsModule.compose.icons,

                projects.ylcsModule.compose.components.urlImage,
                projects.ylcsModule.compose.components.richText,
            )
        }
    }
})