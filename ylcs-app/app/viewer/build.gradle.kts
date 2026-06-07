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
                ExportLib,
                projects.ylcsModule.compose.components.webview,
                projects.ylcsModule.compose.components.media
            )
        }
    }
})

patchMMKVSwiftPackage()