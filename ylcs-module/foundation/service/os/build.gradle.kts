plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.androidLibraryNew,
        libs.plugins.kotlinSerialization,
        libs.plugins.mavenPublish,
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

        webMain.configure(commonMain)

        jsMain.configure(webMain)

        wasmJsMain.configure(webMain)
    }
})