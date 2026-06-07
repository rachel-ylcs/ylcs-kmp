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
                projects.ylcsApp.app.global,
                projects.ylcsApp.app.gameGuessLyrics,
                projects.ylcsApp.app.gameRhyme,

                projects.ylcsModule.compose.components.paginationLayout
            )
        }
    }
})

patchMMKVSwiftPackage()