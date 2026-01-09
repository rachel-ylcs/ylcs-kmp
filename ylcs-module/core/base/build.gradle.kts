plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.androidLibraryNew,
        libs.plugins.kotlinSerialization,
        libs.plugins.mavenPublish,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override val androidNativeTarget: Boolean = true
    override val windowsTarget: Boolean = true
    override val linuxTarget: Boolean = true
    override val macosTarget: Boolean = true

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

        val kotlinMain by create(commonMain)

        val jvmMain by create(clientMain)

        appleMain.configure(skikoMain, clientMain)

        androidMain.configure(jvmMain) {
            lib(
                ExportLib,
                libs.kotlinx.coroutines.android
            )
        }

        iosMain.configure(appleMain, kotlinMain)

        iosMainList.configure(iosMain)

        // appleMain是原生objc，desktopMain是基于jvm的，不许在这加appleMain了:(
        desktopMain.configure(skikoMain, jvmMain) {
            lib(
                ExportLib,
                libs.kotlinx.coroutines.swing
            )
        }

        wasmJsMain.configure(skikoMain, kotlinMain) {
            lib(
                ExportLib,
                libs.kotlinx.broswer
            )
        }

        windowsMain.configure(kotlinMain)

        linuxMain.configure(kotlinMain)

        macosMain.configure(kotlinMain)
    }
})