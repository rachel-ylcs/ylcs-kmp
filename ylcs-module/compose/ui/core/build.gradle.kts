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
                libs.compose.components.resources,
                libs.compose.navigation.event,
                ExportLib,
                projects.ylcsModule.foundation.context,
                projects.ylcsModule.compose.device,
                projects.ylcsModule.compose.theme,
                libs.compose.animation,
                libs.compose.foundation,
                libs.compose.material3,
            )
        }

        desktopMain.configure(commonMain)
    }
})