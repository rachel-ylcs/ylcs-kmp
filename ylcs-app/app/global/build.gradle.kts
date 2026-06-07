plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.androidLibraryNew,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                libs.compose.resources,

//                libs.tool.haze,
//                libs.tool.haze.blur,
                projects.ylcsApp.app.hazeFix,

                ExportLib,

                libs.tool.reorder,

                projects.ylcsApp.cs,
                projects.ylcsApp.mod,

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

patchMMKVSwiftPackage()