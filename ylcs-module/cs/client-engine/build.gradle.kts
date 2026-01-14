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
                libs.ktor.json,
                libs.ktor.client.negotiation,
                libs.ktor.client.websockets,
                ExportLib,
                projects.ylcsModule.cs.core,
                libs.ktor.client,
            )
        }

        androidMain.configure(commonMain) {
            lib(libs.ktor.okhttp)
        }

        val appleMain by create(commonMain) {
            lib(libs.ktor.apple)
        }

        iosMain.configure(appleMain)

        iosMainList.configure(iosMain)

        desktopMain.configure(commonMain) {
            lib(libs.ktor.okhttp)
        }

        webMain.configure(commonMain) {
            lib(libs.ktor.js)
        }

        jsMain.configure(webMain)

        wasmJsMain.configure(webMain)

        androidNativeMain.configure(commonMain) {
            lib(libs.ktor.cio)
        }

        windowsMain.configure(commonMain) {
            lib(libs.ktor.windows)
        }

        linuxMain.configure(commonMain) {
            lib(libs.ktor.cio)
        }

        macosMain.configure(appleMain)
    }
})