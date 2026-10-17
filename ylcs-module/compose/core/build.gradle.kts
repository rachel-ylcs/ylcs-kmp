plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.androidLibrary,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                ExportLib,
                libs.androidx.savedstate,
                libs.androidx.viewmodel,
                libs.androidx.lifecycle,
                libs.compose.runtime,
                libs.compose.ui,
                libs.compose.foundation,

                projects.ylcsModule.foundation.context,
            )
        }

        appleMain.configure(commonMain)

        val skikoMain = createSkiko(commonMain) {
            lib(
                ExportLib,
                libs.skiko
            )
        }

        iosMain.configure(skikoMain, appleMain)

        iosMainList.configure(iosMain)

        desktopMain.configure(skikoMain)

        webMain.configure(skikoMain)

        jsMain.configure(webMain)

        wasmJsMain.configure(webMain)
    }
})