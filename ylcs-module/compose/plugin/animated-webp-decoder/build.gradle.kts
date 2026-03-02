plugins {
    install(
        libs.plugins.kotlinMultiplatform,
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
                projects.ylcsModule.compose.core,
            )
        }

        androidMain.configure(commonMain) {
            lib(
                projects.ylcsModule.compose.plugin.animatedWebpDecoderNdk,
                projects.ylcsModule.platform.nativeLibLoader
            )
        }

        val skikoMain by create(commonMain)

        iosMainList.configure(skikoMain)

        desktopMain.configure(skikoMain)

        webMain.configure(skikoMain)

        jsMain.configure(webMain)

        wasmJsMain.configure(webMain)
    }
})