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
                projects.ylcsModule.foundation.startup,
            )
        }

        val jvmMain by create(commonMain)

        androidMain.configure(jvmMain)

        iosMain.configure(commonMain)

        iosMainList.configure(iosMain)

        desktopMain.configure(jvmMain)

        webMain.configure(commonMain)

        jsMain.configure(webMain)

        wasmJsMain.configure(webMain)

        nativeMain.configure(commonMain)

        androidNativeMain.configure(nativeMain)

        windowsMain.configure(nativeMain)

        linuxMain.configure(nativeMain)

        macosMain.configure(nativeMain)
    }
})