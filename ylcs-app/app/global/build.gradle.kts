plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.androidLibrary,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                libs.compose.resources,

                libs.haze,
                libs.haze.blur,

                ExportLib,

                projects.ylcsApp.cs,
                projects.ylcsApp.mod,
                projects.ylcsApp.thirdParty,

                projects.ylcsModule.foundation.cryptography,

                projects.ylcsModule.cs.clientEngine,

                projects.ylcsModule.compose.startup.picker,
                projects.ylcsModule.compose.startup.exception,
                projects.ylcsModule.compose.startup.config,

                projects.ylcsModule.compose.app,
                projects.ylcsModule.compose.cache,
                projects.ylcsModule.compose.icons,
                projects.ylcsModule.compose.screen,

                projects.ylcsModule.compose.components.lottie,
                projects.ylcsModule.compose.components.richText,
                projects.ylcsModule.compose.components.urlImage,
            )
        }
    }
})
