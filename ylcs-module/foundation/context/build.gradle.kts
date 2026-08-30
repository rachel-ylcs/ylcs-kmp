plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.androidLibraryNew,
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
                ExportLib,
                projects.ylcsModule.core,
            )
        }

        androidMain.configure(commonMain) {
            lib(
                ExportLib,
                libs.androidx.activity,
            )
        }

        val skikoMain = createSkiko(commonMain)

        iosMain.configure(skikoMain)

        iosMainList.configure(iosMain)

        desktopMain.configure(skikoMain)

        webMain.configure(skikoMain)

        jsMain.configure(webMain)

        wasmJsMain.configure(webMain)

        nativeMain.configure(commonMain)

        androidNativeMain.configure(nativeMain)

        windowsMain.configure(nativeMain)

        linuxMain.configure(nativeMain)

        macosMain.configure(nativeMain)
    }
})