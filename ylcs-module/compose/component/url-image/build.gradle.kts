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
    override val exportResource: Boolean = true

    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                projects.ylcsModule.foundation.net.clientEngine,
                libs.sketch,
                libs.sketch.http,
                libs.sketch.resources,
                libs.sketch.gif,
                libs.sketch.webp,
                libs.sketch.extensions.compose,
                libs.sketch.zoom,
                libs.compose.components.resources,
                ExportLib,
                projects.ylcsModule.foundation.framework.startup,
                projects.ylcsModule.compose.ui,
            )
        }

        iosMain.configure(commonMain)

        iosMainList.configure(iosMain)
    }
})