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
    override val namespace: String = "module.compose.ui"
    override val resourceName: String = "compose.ui"

    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                libs.compose.components.resources,
                libs.compose.navigation.event,
                ExportLib,
                projects.ylcsModule.compose.context,
                projects.ylcsModule.compose.device,
                projects.ylcsModule.compose.theme,
                libs.compose.material3,
                libs.compose.material3.icons,
                libs.compose.material3.iconsExtended,
                libs.compose.ui,
            )
        }

        iosMain.configure(commonMain)

        iosMainList.configure(iosMain)
    }
})