plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinCocoapods,
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
                projects.ylcsModule.foundation.startup,
            )
        }

        androidMain.configure(commonMain) {
            lib(libs.mmkv.android)
        }

        iosMain.configure(commonMain)

        iosMainList.configure(iosMain)

        desktopMain.configure(commonMain) {
            lib(projects.ylcsModule.platform.nativeLibLoader)
        }

        webMain.configure(commonMain)

        jsMain.configure(webMain)

        wasmJsMain.configure(webMain)
    }

    override val cocoapodsList: List<Pod> = listOf(
        pod("MMKV", libs.versions.mmkv)
    )
})