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
                libs.kotlinx.atomicfu,
                ExportLib,
                libs.androidx.annotation,
                libs.kotlinx.coroutines,
                libs.kotlinx.io,
                libs.kotlinx.datetime,
                libs.kotlinx.json,
            )
        }

        commonTest.configure {
            lib(
                libs.test,
                libs.kotlinx.coroutines.test
            )
        }

        val clientMain by create(commonMain)

        nativeMain.configure(clientMain)

        val jvmMain by create(clientMain) {
            lib(
                ExportLib,
                libs.androidx.collection,
            )
        }

        appleMain.configure(nativeMain) {
            lib(
                ExportLib,
                libs.androidx.collection,
            )
        }

        androidMain.configure(jvmMain) {
            lib(
                ExportLib,
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
                libs.androidx.collection,
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

        windowsMain.configure(nativeMain) {
            lib(
                ExportLib,
                libs.androidx.collection,
            )
        }

        linuxMain.configure(nativeMain) {
            lib(
                ExportLib,
                libs.androidx.collection,
            )
        }

        macosMain.configure(appleMain)
    }
})