plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.androidLibraryNew,
        libs.plugins.mavenPublish,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override val androidNativeTarget: Boolean = true
    override val windowsTarget: Boolean = true
    override val linuxTarget: Boolean = true
    override val macosTarget: Boolean = true

    override fun KotlinMultiplatformSourceSetsScope.source() {
        val posixMain by create(commonMain)

        val jvmMain by create(commonMain)

        androidMain.configure(jvmMain)

        iosMain.configure(posixMain)

        iosMainList.configure(iosMain)

        desktopMain.configure(jvmMain)

        webMain.configure(commonMain)

        jsMain.configure(webMain)

        wasmJsMain.configure(webMain)

        androidNativeMain.configure(posixMain)

        windowsMain.configure(commonMain)

        linuxMain.configure(posixMain)

        macosMain.configure(posixMain)
    }
})