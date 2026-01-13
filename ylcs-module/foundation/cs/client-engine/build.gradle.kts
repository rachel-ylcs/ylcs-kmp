plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.androidLibraryNew,
        libs.plugins.kotlinSerialization,
        libs.plugins.mavenPublish,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                libs.ktor.json,
                libs.ktor.client.negotiation,
                libs.ktor.client.websockets,
                ExportLib,
                projects.ylcsModule.core.cs,
                libs.ktor.client,
            )
        }

        androidMain.configure(commonMain) {
            lib(
                ExportLib,
                libs.ktor.okhttp,
            )
        }

        iosMain.configure(commonMain) {
            lib(libs.ktor.apple)
        }

        iosMainList.configure(iosMain)

        desktopMain.configure(commonMain) {
            lib(
                ExportLib,
                libs.ktor.okhttp,
            )
        }

        webMain.configure(commonMain) {
            lib(libs.ktor.js)
        }

        jsMain.configure(webMain)

        wasmJsMain.configure(webMain)
    }
})