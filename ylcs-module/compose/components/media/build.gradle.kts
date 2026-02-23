plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.androidLibraryNew,
        libs.plugins.kotlinSerialization,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                ExportLib,
                projects.ylcsModule.foundation.orientation,
                projects.ylcsModule.compose.ui,
            )
        }

        androidMain.configure(commonMain) {
            lib(
                files("libs/android/lib-decoder-ffmpeg.aar"),
                ExportLib,
                libs.media3.ui,
                libs.media3.player,
                libs.media3.session,
            )
        }

        iosMain.configure(commonMain)

        iosMainList.configure(iosMain)

        desktopMain.configure(commonMain) {
            lib(projects.ylcsModule.platform.nativeLibLoader)
        }

        webMain.configure(commonMain) {
            lib(projects.ylcsModule.compose.platformView)
        }

        jsMain.configure(webMain)

        wasmJsMain.configure(webMain)
    }
})