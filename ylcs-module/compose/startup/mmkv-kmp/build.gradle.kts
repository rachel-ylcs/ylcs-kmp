plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.androidLibrary,
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

    // 仅导入 MMKV 的公开 API，避免扫描 MMKVCoreOpenSSL 中需要宏定义的内部模板头文件。
    override val swiftDiscoverClangModulesImplicitly: Boolean = false

    override val swiftPackages: List<SwiftPackage> = listOf(
        SwiftPackage.Remote("https://github.com/Tencent/MMKV.git", libs.versions.mmkv.get(), listOf("MMKV")),
        SwiftPackage.Local("Dummy"),
    )
})
