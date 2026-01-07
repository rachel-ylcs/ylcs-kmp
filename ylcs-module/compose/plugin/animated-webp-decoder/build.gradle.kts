plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.androidLibrary,
        libs.plugins.kotlinSerialization,
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

        androidMain.configure(commonMain)

        val skikoMain by create(commonMain)

        iosMainList.configure(skikoMain)

        desktopMain.configure(skikoMain)

        wasmJsMain.configure(skikoMain)
    }
})