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
    override val namespace: String = "base.compose_core"

    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                ExportLib,
                projects.ylcsBase.core,
                libs.compose.runtime,
                libs.compose.foundation,
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

        desktopMain.configure(skikoMain) {
            lib(
                ExportLib,
                extension.compose.desktop.currentOs
            )
        }

        wasmJsMain.configure(skikoMain)
    }
})