plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.androidLibraryNew,
        libs.plugins.kotlinSerialization,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override val namespace: String = "base.core"

    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                ExportLib,
                libs.kotlinx.atomicfu,
                libs.kotlinx.coroutines,
                libs.kotlinx.datetime,
                libs.kotlinx.io,
                libs.kotlinx.json,
            )
        }

        val skikoMain by create(commonMain)

        val clientMain by create(commonMain)

        val nativeMain by create(commonMain)

        val jvmMain by create(clientMain)

        appleMain.configure(skikoMain, clientMain)

        androidMain.configure(jvmMain) {
            lib(
                ExportLib,
                libs.kotlinx.coroutines.android
            )
        }

        iosMain.configure(appleMain, nativeMain)

        iosMainList.configure(iosMain)

        // appleMain是原生objc，desktopMain是基于jvm的，不许在这加appleMain了:(
        desktopMain.configure(skikoMain, jvmMain) {
            lib(
                ExportLib,
                libs.kotlinx.coroutines.swing
            )
        }

        wasmJsMain.configure(skikoMain, nativeMain) {
            lib(
                ExportLib,
                libs.kotlinx.broswer
            )
        }
    }
})