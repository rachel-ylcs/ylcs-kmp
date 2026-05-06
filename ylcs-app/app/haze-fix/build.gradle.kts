plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.androidLibraryNew,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(projects.ylcsModule.compose.core)
        }

        val skikoMain by create(commonMain)

        iosMainList.configure(skikoMain)

        desktopMain.configure(skikoMain)

        jsMain.configure(skikoMain)

        wasmJsMain.configure(skikoMain)
    }
})