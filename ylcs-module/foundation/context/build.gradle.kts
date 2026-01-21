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
                libs.compose.activity,
            )
        }

        val nonAndroidMain by create(commonMain)

        iosMain.configure(nonAndroidMain)

        iosMainList.configure(iosMain)

        desktopMain.configure(nonAndroidMain)

        webMain.configure(nonAndroidMain)

        jsMain.configure(webMain)

        wasmJsMain.configure(webMain)

        nativeMain.configure(nonAndroidMain)

        androidNativeMain.configure(nativeMain)

        windowsMain.configure(nativeMain)

        linuxMain.configure(nativeMain)

        macosMain.configure(nativeMain)
    }
})