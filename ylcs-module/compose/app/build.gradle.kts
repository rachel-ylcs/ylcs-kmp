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
    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                projects.ylcsModule.platform.nativeLibLoader,
                libs.compose.components.resources,
                ExportLib,
                projects.ylcsModule.foundation.framework.startup,
                projects.ylcsModule.compose.ui,
            )
        }

        iosMain.configure(commonMain)

        iosMainList.configure(iosMain)
    }
})