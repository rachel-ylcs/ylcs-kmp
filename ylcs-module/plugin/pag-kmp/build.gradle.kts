import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMImportExtension

plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.androidLibraryNew,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                ExportLib,
                projects.ylcsModule.compose.platformView,
            )
        }

        androidMain.configure(commonMain) {
            lib(
                ExportLib,
                libs.pag.android
            )
        }

        iosMain.configure(commonMain)

        iosMainList.configure(iosMain)

        desktopMain.configure(commonMain) {
            lib(projects.ylcsModule.platform.nativeLibLoader)
        }

        webMain.configure(commonMain) {
            lib(npm("libpag", "4.5.16"))
        }

        jsMain.configure(webMain)

        wasmJsMain.configure(webMain)
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    override fun SwiftPMImportExtension.swiftPMDependencies() {
        swiftPackage(
            url = "https://github.com/libpag/pag-ios.git",
            version = libs.versions.pag.get(),
            products = listOf("libpag"),
        )
    }
})