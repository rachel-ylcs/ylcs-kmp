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
                projects.ylcsModule.compose.platformView,
            )
        }

        androidMain.configure(commonMain) {
            lib(
                ExportLib,
                libs.pag.android
            )
        }

        iosMain.configure(commonMain)

        iosMainList.configure(iosMain)

        desktopMain.configure(commonMain) {
            lib(projects.ylcsModule.platform.nativeLibLoader)
        }

        webMain.configure(commonMain) {
            lib(npm("libpag", "4.5.16"))
        }

        jsMain.configure(webMain)

        wasmJsMain.configure(webMain)
    }

    override val cocoapodsList: List<Pod> = listOf(
        pod("libpag", libs.versions.pag)
    )
})