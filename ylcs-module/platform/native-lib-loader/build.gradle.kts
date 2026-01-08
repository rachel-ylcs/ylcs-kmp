plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.androidLibraryNew,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override fun KotlinMultiplatformSourceSetsScope.source() {
        val jvmMain by create(commonMain)

        androidMain.configure(jvmMain)

        iosMain.configure(commonMain)

        iosMainList.configure(iosMain)

        desktopMain.configure(jvmMain)
    }
})