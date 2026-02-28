plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.androidLibraryNew,
        libs.plugins.kotlinSerialization,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                projects.ylcsModule.cs.clientEngine,
                libs.sketch,
                libs.sketch.http,
                libs.sketch.resources,
                libs.sketch.gif,
                libs.sketch.webp,
                libs.sketch.extensions.compose,
                libs.sketch.zoom,
                libs.compose.resources,
                ExportLib,
                projects.ylcsModule.foundation.startup,
                projects.ylcsModule.compose.ui,
            )
        }

        iosMain.configure(commonMain)

        iosMainList.configure(iosMain)

        webMain.configure(commonMain)

        jsMain.configure(webMain)

        wasmJsMain.configure(webMain)
    }
})