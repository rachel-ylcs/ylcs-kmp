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
    override val namespace: String = "module.compose.component.url_image"
    override val resourceName: String = "compose.component.url_image"

    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                projects.ylcsModule.clientEngine,
                libs.sketch,
                libs.sketch.http,
                libs.sketch.resources,
                libs.sketch.gif,
                libs.sketch.webp,
                libs.sketch.extensions.compose,
                libs.sketch.zoom,
                libs.compose.components.resources,
                ExportLib,
                projects.ylcsModule.compose.ui,
                projects.ylcsModule.compose.startup,
            )
        }

        iosMain.configure(commonMain)

        iosMainList.configure(iosMain)
    }
})