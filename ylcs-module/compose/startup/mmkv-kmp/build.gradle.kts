import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMImportExtension

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
    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                ExportLib,
                projects.ylcsModule.foundation.filesystem,
                projects.ylcsModule.foundation.startup,
            )
        }

        androidMain.configure(commonMain) {
            lib(libs.mmkv.android)
        }

        iosMain.configure(commonMain)

        iosMainList.configure(iosMain)

        desktopMain.configure(commonMain) {
            lib(projects.ylcsModule.platform.nativeLibLoader)
        }

        webMain.configure(commonMain)

        jsMain.configure(webMain)

        wasmJsMain.configure(webMain)
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    override fun SwiftPMImportExtension.swiftPMDependencies() {
        swiftPackage(
            url = "https://github.com/Tencent/MMKV.git",
            version = libs.versions.mmkv.get(),
            products = listOf("MMKV"),
        )
    }
})

patchMMKVSwiftPackage()