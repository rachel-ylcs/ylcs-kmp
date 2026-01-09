plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinCocoapods,
        libs.plugins.androidLibraryNew,
        libs.plugins.kotlinSerialization,
        libs.plugins.mavenPublish,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                ExportLib,
                projects.ylcsModule.foundation.framework.startup,
            )
        }

        androidMain.configure(commonMain) {
            lib(
                libs.mmkv.android
            )
        }

        iosMain.configure(commonMain)

        iosMainList.configure(iosMain)

        desktopMain.configure(commonMain) {
            lib(
                projects.ylcsModule.platform.nativeLibLoader
            )
        }
    }

    override val cocoapodsList: List<Pod> = listOf(
        Pod("MMKV", version = libs.versions.mmkv)
    )
})