plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinCocoapods,
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
                projects.ylcsModule.compose.ui,
                projects.ylcsModule.compose.platformView,

                libs.tool.qrcode,
            )
        }

        androidMain.configure(commonMain) {
            lib(
                libs.scan.android,
                libs.scan.camera.android
            )
        }

        iosMain.configure(commonMain)

        iosMainList.configure(iosMain)

        webMain.configure(commonMain)

        jsMain.configure(webMain)

        wasmJsMain.configure(webMain)
    }

    override val cocoapodsList: List<Pod> = listOf(
        pod("SGQRCode", libs.versions.sgQrcode),
    )
})