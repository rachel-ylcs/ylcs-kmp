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
                libs.tool.blur,
                libs.tool.reorder,

                projects.ylcsApp.app.global,
                projects.ylcsApp.app.thirdParty,
                projects.ylcsApp.app.viewer,

                projects.ylcsModule.compose.components.paginationLayout,
                projects.ylcsModule.compose.components.media,
            )
        }

        iosMain.configure(commonMain)

        iosMainList.configure(iosMain)

        webMain.configure(commonMain)

        jsMain.configure(webMain)

        wasmJsMain.configure(webMain)
    }
})