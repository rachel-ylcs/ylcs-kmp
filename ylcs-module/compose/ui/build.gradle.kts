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
                libs.compose.material3.iconsExtended,
                libs.compose.material3,
                libs.compose.resources,
                libs.compose.navigation.event,
                ExportLib,
                projects.ylcsModule.compose.theme,
                libs.compose.animation,
            )
        }

        iosMain.configure(commonMain)

        iosMainList.configure(iosMain)

        webMain.configure(commonMain)

        jsMain.configure(webMain)

        wasmJsMain.configure(webMain)
    }
})