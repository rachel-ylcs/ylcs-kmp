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
                projects.ylcsApp.app.global,
                projects.ylcsApp.app.community,
                projects.ylcsApp.app.information,
                projects.ylcsApp.app.viewer,

                projects.ylcsModule.compose.components.paginationLayout,
                projects.ylcsModule.compose.components.qrcode,
            )
        }
    }
})