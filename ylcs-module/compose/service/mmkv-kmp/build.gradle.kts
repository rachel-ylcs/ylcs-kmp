plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinCocoapods,
        libs.plugins.androidLibraryNew,
        libs.plugins.kotlinSerialization,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override val namespace: String = "module.compose.service.mmkv_kmp"
    override val buildCocoapods: Boolean = true

    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                ExportLib,
                projects.ylcsModule.compose.startup,
            )
        }

        androidMain.configure(commonMain) {
            lib(libs.mmkv.android)
        }

        iosMain.configure(commonMain)

        iosMainList.configure(iosMain)
    }

    override val cocoapodsList: List<Pod> = listOf(
        Pod("MMKV", version = libs.versions.mmkv)
    )
})