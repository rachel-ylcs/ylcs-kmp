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
                projects.ylcsModule.core.base,
                libs.compose.runtime,
                libs.compose.ui,
                libs.compose.collection,
                libs.compose.savedstate,
                libs.compose.viewmodel,
                libs.compose.lifecycle,
            )
        }

        commonTest.configure {
            lib(
                libs.test,
                libs.kotlinx.coroutines.test,
            )
        }

        androidMain.configure(commonMain) {
            lib(
                ExportLib,
                libs.compose.activity,
                libs.compose.ui.graphics.android
            )
        }

        val skikoMain by create(commonMain) {
            lib(
                ExportLib,
                libs.skiko
            )
        }

        iosMain.configure(skikoMain)

        iosMainList.configure(iosMain)

        desktopMain.configure(skikoMain)

        webMain.configure(skikoMain)

        jsMain.configure(webMain)

        wasmJsMain.configure(webMain)
    }
})