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
                projects.ylcsApp.app.global,
                projects.ylcsApp.app.gameGuessLyrics,
                projects.ylcsApp.app.gameRhyme,

                projects.ylcsModule.compose.components.paginationLayout
            )
        }
    }
})