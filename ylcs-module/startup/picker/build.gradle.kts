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
    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                ExportLib,
                projects.ylcsModule.foundation.startup,
            )
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
})