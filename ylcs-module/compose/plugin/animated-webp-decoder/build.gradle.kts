plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.androidLibrary,
        libs.plugins.kotlinSerialization,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override val buildNDK: Boolean = true
    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                ExportLib,
                projects.ylcsModule.core.compose,
            )
        }

        androidMain.configure(commonMain) {
            lib(
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