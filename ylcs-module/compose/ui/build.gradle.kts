plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.androidLibraryNew,
        libs.plugins.kotlinSerialization,
        libs.plugins.mavenPublish,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override val exportResource: Boolean = true

    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                libs.compose.components.resources,
                libs.compose.navigation.event,
                ExportLib,
                projects.ylcsModule.foundation.context,
                projects.ylcsModule.compose.device,
                projects.ylcsModule.compose.theme,
                libs.compose.material3,
                libs.compose.material3.icons,
                libs.compose.material3.iconsExtended,
            )
        }

        iosMain.configure(commonMain)

        iosMainList.configure(iosMain)

        webMain.configure(commonMain)

        jsMain.configure(webMain)

        wasmJsMain.configure(webMain)
    }
})