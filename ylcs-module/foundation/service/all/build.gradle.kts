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
                ExportLib,
                projects.ylcsModule.foundation.service.exception,
                projects.ylcsModule.foundation.service.mmkvKmp,
                projects.ylcsModule.foundation.service.os,
                projects.ylcsModule.foundation.service.picker,
            )
        }
    }
})