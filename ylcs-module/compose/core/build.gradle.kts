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
                ExportLib,
                projects.ylcsModule.core,
                libs.compose.runtime,
                libs.compose.ui,
                libs.compose.collection,
                libs.compose.savedstate,
                libs.compose.viewmodel,
                libs.compose.lifecycle,
            )
        }

        androidMain.configure(commonMain) {
            lib(
                ExportLib,
                libs.compose.activity,
            )
        }

        appleMain.configure(commonMain)

        val skikoMain by create(commonMain) {
            lib(
                ExportLib,
                libs.skiko
            )
        }

        iosMain.configure(skikoMain, appleMain)

        iosMainList.configure(iosMain)

        desktopMain.configure(skikoMain) {
            lib(composeOSLib)
        }

        webMain.configure(skikoMain)

        jsMain.configure(webMain)

        wasmJsMain.configure(webMain)
    }
})