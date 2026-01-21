plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.androidLibraryNew,
        libs.plugins.kotlinSerialization,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
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

        val clientMain by create(commonMain)

        nativeMain.configure(clientMain)

        val jvmMain by create(clientMain)

        appleMain.configure(nativeMain)

        androidMain.configure(jvmMain) {
            lib(
                ExportLib,
                libs.compose.annotation,
                libs.kotlinx.coroutines.android,
            )
        }

        iosMain.configure(appleMain)

        iosMainList.configure(iosMain)

        // appleMain是原生objc，desktopMain是基于jvm的，不许在这加appleMain了:(
        desktopMain.configure(jvmMain) {
            lib(
                ExportLib,
                libs.kotlinx.coroutines.swing
            )
        }

        webMain.configure(commonMain) {
            lib(
                ExportLib,
                libs.kotlinx.broswer
            )
        }

        jsMain.configure(webMain)

        jsTest.configure {
            lib(
                libs.test,
                libs.kotlinx.coroutines.test
            )
        }

        wasmJsMain.configure(webMain)

        wasmJsTest.configure {
            lib(
                libs.test,
                libs.kotlinx.coroutines.test
            )
        }

        androidNativeMain.configure(nativeMain)

        windowsMain.configure(nativeMain)

        linuxMain.configure(nativeMain)

        macosMain.configure(appleMain)
    }
})