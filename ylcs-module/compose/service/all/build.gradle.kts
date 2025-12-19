plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.androidLibraryNew,
        libs.plugins.kotlinSerialization,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override val namespace: String = "module.compose.service.all"

    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                ExportLib,
                projects.ylcsModule.compose.service.exception,
                projects.ylcsModule.compose.service.os,
                projects.ylcsModule.compose.service.mmkvKmp,
                projects.ylcsModule.compose.service.config,
                projects.ylcsModule.compose.service.picker,
            )
        }
    }
})